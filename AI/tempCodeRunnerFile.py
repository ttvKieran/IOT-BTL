import os
import uvicorn
import logging # Thêm import logging
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
    # GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
    GEMINI_API_KEY = "AIzaSyBTEx5oDoV-FHYZ9EOvjHAZ3al8qQYZENA"
    if GEMINI_API_KEY is None:
        raise KeyError
except KeyError:
    # In ra thông báo lỗi rõ ràng nếu không tìm thấy key
    logging.critical("GEMINI_API_KEY không được tìm thấy. Vui lòng đặt biến môi trường.")
    raise EnvironmentError("GEMINI_API_KEY không được tìm thấy. Vui lòng đặt biến môi trường.")

genai.configure(api_key=GEMINI_API_KEY)

# --- 2. Định nghĩa "Công cụ" (Tools) ---
tools = [
    {
        "name": "getDeviceState",
        "description": "Lấy trạng thái cảm biến (nhiệt độ, độ ẩm, ánh sáng, độ ẩm đất) và trạng thái điều khiển (bơm, đèn) hiện tại của một thiết bị vườn. Dùng khi người dùng hỏi 'vườn thế nào?', 'kiểm tra vườn', 'nhiệt độ?', v.v.",
        "parameters": {
            # (SỬA LỖI SCHEMA: Phải là chữ thường)
            "type": "OBJECT", 
            "properties": {
                "deviceUid": {
                    "type": "STRING", # (Sửa lỗi)
                    "description": "Mã UID của thiết bị, ví dụ: ESP32_GARDEN_01"
                }
            },
            "required": ["deviceUid"]
        }
    },
    {
        "name": "controlDevice",
        "description": "Bật hoặc Tắt một thiết bị (bơm hoặc đèn). Dùng khi người dùng ra lệnh 'tưới cây', 'bật đèn', 'tắt bơm'.",
        "parameters": {
            "type": "OBJECT", # (Sửa lỗi)
            "properties": {
                "deviceUid": {
                    "type": "STRING", # (Sửa lỗi)
                    "description": "Mã UID của thiết bị, ví dụ: ESP32_GARDEN_01"
                },
                "deviceName": {
                    "type": "STRING", # (Sửa lỗi)
                    "description": "Tên thiết bị (PUMP hoặc LIGHT)"
                },
                "turnOn": {
                    "type": "BOOLEAN", # (Sửa lỗi)
                    "description": "Bật (true) hay Tắt (false)"
                }
            },
            "required": ["deviceUid", "deviceName", "turnOn"]
        }
    }
]

# Khởi tạo mô hình Gemini
model = genai.GenerativeModel(
    model_name='gemini-2.0-flash', 
    generation_config={"temperature": 0.7},
    tools=tools
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
        {request.weather_context.model_dump_json(indent=2, by_alias=True)}

        ## NHIỆM VỤ CỦA BẠN:
        1. Trả lời thân thiện, lịch sự.
        2. Nếu người dùng chỉ hỏi thông tin (ví dụ: "vườn sao rồi?", "nhiệt độ?"),
           HÃY SỬ DỤNG DỮ LIỆU TRONG 'BỐI CẢNH' để trả lời.
           (Ví dụ: "Chào bạn, nhiệt độ trong vườn đang là {request.garden_context.sensors.temperature}°C,
           và thời tiết bên ngoài trời {request.weather_context.description}, {request.weather_context.temperature}°C.")
           KHÔNG gọi hàm `getDeviceState` nếu đã có bối cảnh.
        3. Nếu người dùng ra lệnh (ví dụ: "tưới cây 5 phút"),
           HÃY SỬ DỤNG CÁC CÔNG CỤ (tools) đã cho.
        4. (Quan trọng) Nếu đất khô (soil_moisture < 40) nhưng BỐI CẢNH THỜI TIẾT báo sắp mưa
           (ví dụ: "mưa rào", "mưa giông"), HÃY TỪ CHỐI TƯỚI và giải thích lý do.
           (Ví dụ: "Đất đang hơi khô, nhưng dự báo thời tiết báo sắp có mưa,
           vì vậy tôi sẽ không tưới bây giờ để tiết kiệm nước.")
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

        if part.function_call:
            # TRƯỜNG HỢP 2: AI YÊU CẦU GỌI HÀM
            func_call = part.function_call
            
            tool_response = ToolCallResponse(
                tool_name=func_call.name,
                arguments=dict(func_call.args)
            )
            return ChatResponse(
                response_type="TOOL_CALL",
                tool_call=tool_response
            )
        else:
            # TRƯỜN HỢP 1: AI TRẢ LỜI BẰNG TEXT
            return ChatResponse(
                response_type="TEXT",
                text_content=part.text
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