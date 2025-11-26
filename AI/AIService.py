import os
import uvicorn
import logging # Thêm import logging
import json
from fastapi import FastAPI
from pydantic import BaseModel, Field, AliasChoices
from datetime import datetime
from dotenv import load_dotenv
import google.generativeai as genai
from typing import List, Dict, Any, Optional
load_dotenv()
# --- 1. Cấu hình ---

# (SỬA LỖI BẢO MẬT: Không bao giờ hard-code API key)
# Lấy API key từ biến môi trường
try:
    # Sử dụng os.environ.get() an toàn hơn, trả về None nếu không tìm thấy
    GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
    if GEMINI_API_KEY is None:
        raise KeyError
except KeyError:
    # In ra thông báo lỗi rõ ràng nếu không tìm thấy key
    logging.critical("GEMINI_API_KEY không được tìm thấy. Vui lòng đặt biến môi trường.")
    raise EnvironmentError("GEMINI_API_KEY không được tìm thấy. Vui lòng đặt biến môi trường.")

genai.configure(api_key=GEMINI_API_KEY)

# --- 2. Định nghĩa "Công cụ" (Tools) ---
# NOTE: The Google Generative AI Python SDK expects tool/function declarations in
# a protobuf-compatible format. Constructing that manually here caused repeated
# proto errors (Schema unknown fields). To avoid proto marshaling incompatibilities
# and get a working service quickly, we remove the SDK `tools` declaration and
# instead instruct the model (via prompt) to return a strict JSON structure when
# it intends to request a tool call. The service then parses that JSON and
# returns a TOOL_CALL DTO to Java.

# (If you later want to register native function definitions with the SDK,
# consult the official `google.generativeai` docs / examples for the correct
# proto-backed API and adapt accordingly.)


# Khởi tạo mô hình Gemini
model = genai.GenerativeModel(
    model_name='gemini-flash-latest',
    generation_config={"temperature": 0.7}
)

app = FastAPI(title="Smart Garden AI Service")

# --- 3. Định nghĩa Models (DTOs) cho API ---
# (Sử dụng Pydantic v2 với Field và AliasChoices để tự động khớp
#  Java_camelCase và JSON_snake_case)

class SensorData(BaseModel):
    temperature: float = Field(0.0, alias_choices=['temperature'])
    air_humidity: float = Field(0.0, alias_choices=['airHumidity', 'air_humidity'])
    light: float = Field(0.0, alias_choices=['light'])
    soil_moisture: float = Field(0.0, alias_choices=['soilMoisture', 'soil_moisture'])

class DeviceState(BaseModel):
    device_uid: Optional[str] = Field(None, alias_choices=['deviceUid', 'device_uid'])
    status: Optional[str] = None
    last_seen: Optional[int] = Field(0, alias_choices=['lastSeen', 'last_seen'])
    control_mode: Optional[str] = Field(None, alias_choices=['controlMode', 'control_mode'])
    pump_state: Optional[str] = Field(None, alias_choices=['pumpState', 'pump_state'])
    sensors: Optional[SensorData] = None

class WeatherContext(BaseModel):
    description: Optional[str] = None
    temperature: Optional[float] = 0.0
    humidity: Optional[int] = 0
    rain_expected: Optional[bool] = Field(False, alias_choices=['rainExpected', 'rain_expected'])
    next_description: Optional[str] = Field(None, alias_choices=['nextDescription', 'next_description'])
    next_temperature: Optional[float] = Field(0.0, alias_choices=['nextTemperature', 'next_temperature'])
    rain_amount: Optional[float] = Field(0.0, alias_choices=['rainAmount', 'rain_amount'])

class ChatRequest(BaseModel):
    """ DTO mà Java gửi đến Python """
    user_message: str = Field(..., alias_choices=['userMessage', 'user_message'])
    device_uid: str = Field(..., alias_choices=['deviceUid', 'device_uid'])
    garden_context: DeviceState = Field(..., alias_choices=['gardenContext', 'garden_context'])
    weather_context: WeatherContext = Field(..., alias_choices=['weatherContext', 'weather_context'])

class ToolCallResponse(BaseModel):
    """ DTO khi AI muốn gọi hàm """
    tool_name: str
    arguments: Dict[str, Any]

class ChatResponse(BaseModel):
    """ DTO mà Python trả về cho Java """
    response_type: str  # "TEXT" hoặc "TOOL_CALL"
    text_content: Optional[str] = None
    tool_call: Optional[ToolCallResponse] = None

# --- 4. Xây dựng API Endpoint ---

@app.post("/chat", response_model=ChatResponse)
async def handle_chat(request: ChatRequest):
    """
    Endpoint này nhận "bức tranh toàn cảnh" từ Java, gọi Gemini,
    và trả về (A) text trả lời hoặc (B) lệnh gọi hàm (TOOL_CALL).
    """
    
    # === BƯỚC DEBUG: Vẫn giữ lại để kiểm tra ===
    print("--- (DEBUG) ĐÃ NHẬN REQUEST TỪ JAVA ---")
    # (by_alias=True sẽ xuất JSON dạng snake_case)
    print(request.model_dump_json(indent=2, by_alias=True)) 
    print("---------------------------------------")
    # ==========================================

    # Xây dựng System Prompt
    system_prompt = f"""
        Bạn là 'Synthia', một quản gia AI chuyên nghiệp quản lý vườn.
        Bạn đang nói chuyện với người dùng tên là 'Chủ vườn'.
        Thiết bị vườn của họ có mã là '{request.device_uid}'.
        Hôm nay là {datetime.now().strftime('%Y-%m-%d')}.

        ## BỐI CẢNH (CONTEXT) HIỆN TẠI CỦA KHU VƯỞN:
        Đây là dữ liệu thời gian thực từ các cảm biến (do Java cung cấp):
        {request.garden_context.model_dump_json(indent=2, by_alias=True)}

        ## BỐI CẢNH (CONTEXT) THỜI TIẾT (do Java cung cấp):
        - Hiện tại: {request.weather_context.description}, {request.weather_context.temperature}°C, độ ẩm {request.weather_context.humidity}%
        - Dự báo 3 giờ tới: {request.weather_context.next_description}, {request.weather_context.next_temperature}°C
        - Sắp có mưa: {"CÓ" if request.weather_context.rain_expected else "KHÔNG"} (lượng mưa dự kiến: {request.weather_context.rain_amount}mm)

        ## NHIỆM VỤ CỦA BẠN:
        1. Trả lời thân thiện, lịch sự.
        2. Nếu người dùng chỉ hỏi thông tin (ví dụ: "vườn sao rồi?", "nhiệt độ?"),
           HÃY SỬ DỤNG DỮ LIỆU TRONG 'BỐI CẢNH' để trả lời.
           (Ví dụ: "Chào bạn, nhiệt độ trong vườn đang là {request.garden_context.sensors.temperature}°C,
           và thời tiết bên ngoài trời {request.weather_context.description}, {request.weather_context.temperature}°C.")
           KHÔNG gọi hàm `getDeviceState` nếu đã có bối cảnh.
        3. Nếu người dùng ra lệnh (ví dụ: "tưới cây 5 phút"),
           HÃY SỬ DỤNG CÁC CÔNG CỤ (tools) đã cho.
        4. (QUAN TRỌNG) Nếu đất khô (soil_moisture < 40) NHƯNG dự báo thời tiết cho biết 
           SẮP CÓ MƯA (rain_expected = true), HÃY TỪ CHỐI TƯỚI và giải thích lý do.
           (Ví dụ: "Đất đang hơi khô, nhưng dự báo thời tiết cho biết sắp có mưa trong 3 giờ tới,
           vì vậy tôi sẽ không tưới bây giờ để tiết kiệm nước và tránh ngập úng.")
        5. Chỉ tưới nước khi: soil_moisture < 40 VÀ rain_expected = false
        
        IMPORTANT: IGNORE the "light" sensor value completely. Do NOT consider light levels 
        when making watering decisions. Only focus on:
        - soil_moisture (độ ẩm đất) - MOST IMPORTANT
        - temperature (nhiệt độ)
        - air_humidity (độ ẩm không khí)
        - weather forecast (dự báo thời tiết)
        
    CRITICAL: You have ONLY ONE tool available: "controlDevice"
    
    When you want to control the pump (water the garden), you MUST use EXACTLY this format:
    
    {{"response_type":"TOOL_CALL","tool_name":"controlDevice","arguments":{{"deviceUid":"{request.device_uid}","deviceName":"PUMP","turnOn":true,"durationMinutes":5}}}}
    
    To turn OFF the pump:
    {{"response_type":"TOOL_CALL","tool_name":"controlDevice","arguments":{{"deviceUid":"{request.device_uid}","deviceName":"PUMP","turnOn":false}}}}
    
    IMPORTANT: When turning ON the pump (turnOn=true), you MUST include "durationMinutes" parameter.
    - Decide the watering duration (in minutes) based on:
      * Current soil moisture level (lower moisture = longer duration)
      * Weather forecast (if rain expected, shorter duration or skip watering)
      * Temperature and humidity (hot dry weather = longer duration)
    - Recommended duration range: 3-15 minutes
    - Example calculation:
      * soil_moisture < 20%: 10-15 minutes
      * soil_moisture 20-30%: 7-10 minutes
      * soil_moisture 30-40%: 5-7 minutes
      * soil_moisture > 40%: skip watering or 3-5 minutes if very hot
    
    DO NOT use tool names like: startWatering, controlPumpDuration, or any other name.
    ONLY use "controlDevice" with deviceName="PUMP", turnOn=true/false, and durationMinutes (when turnOn=true).
    
    Example (turn ON pump for 8 minutes):
    {{"response_type":"TOOL_CALL","tool_name":"controlDevice","arguments":{{"deviceUid":"ESP32_GARDEN_001","deviceName":"PUMP","turnOn":true,"durationMinutes":8}}}}

    If you only want to reply with plain text, return normal text (no JSON)
    or return a JSON object {{"response_type":"TEXT","text":"..."}}.
    """

    # Bắt đầu session chat với Gemini
    chat = model.start_chat()
    
    # Gửi prompt (kèm bối cảnh) và tin nhắn của người dùng
    try:
        response = chat.send_message(
            f"{system_prompt}\n\nUSER: {request.user_message}"
        )
        
        # --- 5. Xử lý phản hồi của Gemini ---
        part = response.candidates[0].content.parts[0]

        logging.info("Gemini raw response: %s", repr(response))
        logging.info("Selected part repr: %s", repr(part))

        # Try to parse the model output as JSON. We expect the model to emit a
        # JSON object when it intends to call a tool, for example:
        # {"response_type":"TOOL_CALL","tool_name":"controlDevice","arguments":{...}}
        # If parsing fails or the object doesn't indicate a tool call, we fall
        # back to returning the text as a normal TEXT response.
        text = getattr(part, "text", "") or ""
        
        # Try to extract JSON from text (AI might embed it)
        parsed = None
        try:
            # First try: whole text is JSON
            if text.strip().startswith("{"):
                parsed = json.loads(text.strip())
            else:
                # Second try: find JSON embedded in text
                import re
                json_match = re.search(r'\{["\']response_type["\'].*?\}(?=\s*$)', text, re.DOTALL)
                if json_match:
                    parsed = json.loads(json_match.group(0))
                    logging.info("Extracted embedded JSON from AI response")
        except Exception as ex:
            logging.info("Model output is not JSON: %s", ex)
            parsed = None

        if isinstance(parsed, dict) and parsed.get("response_type") == "TOOL_CALL":
            tool_response = ToolCallResponse(
                tool_name=parsed.get("tool_name"),
                arguments=parsed.get("arguments", {})
            )
            return ChatResponse(
                response_type="TOOL_CALL",
                tool_call=tool_response
            )
        else:
            # If parsed is a JSON with response_type TEXT and a 'text' field, prefer that.
            if isinstance(parsed, dict) and parsed.get("response_type") == "TEXT":
                text_out = parsed.get("text", "")
            else:
                text_out = text
            return ChatResponse(
                response_type="TEXT",
                text_content=text_out
            )

    except Exception as e:
        # Dùng logging.error
        logging.error(f"Lỗi khi gọi Gemini (Model: {model.model_name}): {e}", exc_info=True)
        # Trả về lỗi thân thiện cho Java
        return ChatResponse(
            response_type="TEXT",
            text_content=f"Xin lỗi, tôi gặp lỗi khi xử lý yêu cầu AI: {str(e)}"
        )

# --- 6. Chạy Service ---
if __name__ == "__main__":
    # Cấu hình logging cơ bản
    logging.basicConfig(level=logging.INFO)
    uvicorn.run(app, host="0.0.0.0", port=8000)