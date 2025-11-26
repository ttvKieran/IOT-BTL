import React, { useState, useEffect, useRef } from 'react';
import Navbar from '../components/Navbar';
import { deviceAPI } from '../services/api';
import ReactMarkdown from 'react-markdown';

const DEVICE_UID = process.env.REACT_APP_DEVICE_UID || 'ESP32_GARDEN_001';
const GEMINI_API_KEY = process.env.REACT_APP_GEMINI_API_KEY || '';

const Chat = () => {
  const [messages, setMessages] = useState([
    {
      role: 'assistant',
      content: 'Xin chào! Tôi là trợ lý AI của hệ thống Smart Garden. Tôi có thể giúp bạn giám sát và chăm sóc vườn thông minh hơn. Hãy hỏi tôi về trạng thái vườn, cách tưới nước, hoặc bất kỳ vấn đề nào bạn gặp phải! 🌱'
    }
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [deviceState, setDeviceState] = useState(null);
  const messagesEndRef = useRef(null);

  useEffect(() => {
    loadDeviceState();
  }, []);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const loadDeviceState = async () => {
    try {
      const response = await deviceAPI.getDeviceState(DEVICE_UID);
      if (response.data && response.data.data) {
        setDeviceState(response.data.data);
      }
    } catch (error) {
      console.error('❌ Error loading device state:', error);
    }
  };

  const buildContextPrompt = () => {
    if (!deviceState) return '';

    const sensors = deviceState.sensors || {};
    const temperature = sensors.temperature ?? 'N/A';
    const airHumidity = sensors.air_humidity ?? 'N/A';
    const soilMoisture = sensors.soil_moisture ?? 'N/A';
    const pumpState = deviceState.pump_state === 'ON' ? 'BẬT' : 'TẮT';
    const controlMode = deviceState.control_mode || 'N/A';

    const context = `
Thông tin hiện tại của vườn:
- Nhiệt độ: ${temperature}°C
- Độ ẩm không khí: ${airHumidity}%
- Độ ẩm đất: ${soilMoisture}%
- Trạng thái máy bơm: ${pumpState}
- Chế độ: ${controlMode}

Bạn là trợ lý AI chuyên về nông nghiệp và chăm sóc cây trồng. Hãy trả lời câu hỏi của người dùng dựa trên dữ liệu hiện tại và kiến thức về canh tác. Trả lời ngắn gọn, dễ hiểu, và đưa ra lời khuyên cụ thể.
`;
    return context;
  };

  const sendMessage = async () => {
    if (!input.trim()) return;

    const userMessage = { role: 'user', content: input };
    setMessages(prev => [...prev, userMessage]);
    setInput('');
    setLoading(true);

    try {
      // Build context with device data
      const contextPrompt = buildContextPrompt();
      
      // Call Gemini API
      const response = await fetch(
        `https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=${GEMINI_API_KEY}`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            contents: [
              {
                parts: [
                  { text: contextPrompt + '\n\nCâu hỏi của người dùng: ' + input }
                ]
              }
            ],
            generationConfig: {
              temperature: 0.7,
              maxOutputTokens: 2048,
            }
          })
        }
      );

      const data = await response.json();
      
      if (data.candidates && data.candidates[0]?.content?.parts?.[0]?.text) {
        const aiResponse = {
          role: 'assistant',
          content: data.candidates[0].content.parts[0].text
        };
        setMessages(prev => [...prev, aiResponse]);
      } else {
        throw new Error('Invalid response from Gemini API');
      }
    } catch (error) {
      console.error('❌ Error calling Gemini API:', error);
      
      // Fallback response if API fails
      const fallbackResponse = {
        role: 'assistant',
        content: `Xin lỗi, tôi gặp lỗi khi xử lý yêu cầu của bạn. ${
          !GEMINI_API_KEY 
            ? 'Vui lòng cấu hình REACT_APP_GEMINI_API_KEY trong file .env để sử dụng tính năng AI Chat.' 
            : 'Vui lòng thử lại sau.'
        }`
      };
      setMessages(prev => [...prev, fallbackResponse]);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  const quickQuestions = [
    'Độ ẩm đất hiện tại thế nào?',
    'Tôi nên tưới nước không?',
    'Nhiệt độ có ổn không?',
    'Cây cần gì để phát triển tốt?'
  ];

  return (
    <div className="min-h-screen bg-gradient-to-br from-purple-50 via-pink-50 to-blue-50" style={{
      backgroundImage: 'url(/img/image.png)',
      backgroundSize: 'cover',
      backgroundAttachment: 'fixed',
      backgroundPosition: 'center'
    }}>
      <div className="absolute inset-0 bg-white/85 backdrop-blur-sm"></div>
      
      <div className="relative z-10">
        <Navbar />
        <div className="max-w-5xl mx-auto px-6 py-8 h-[calc(100vh-80px)] flex flex-col">
        {/* Header */}
        <div className="mb-6">
          <h1 className="text-4xl font-bold text-gray-800 mb-2">AI Chat</h1>
          <p className="text-gray-600">Trò chuyện với trợ lý AI về vườn của bạn</p>
        </div>

        {/* Chat Container */}
        <div className="flex-1 bg-gradient-to-br from-white/90 via-purple-50/80 to-pink-50/80 backdrop-blur-xl rounded-2xl border-2 border-white/50 shadow-2xl flex flex-col overflow-hidden relative">
          {/* Decorative Elements */}
          <div className="absolute top-0 left-0 w-full h-full pointer-events-none overflow-hidden">
            <div className="absolute -top-20 -right-20 w-60 h-60 bg-purple-300/20 rounded-full blur-3xl"></div>
            <div className="absolute -bottom-20 -left-20 w-60 h-60 bg-pink-300/20 rounded-full blur-3xl"></div>
            <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-blue-300/10 rounded-full blur-3xl"></div>
          </div>

          {/* Messages Area */}
          <div className="flex-1 overflow-y-auto p-6 space-y-4 relative z-10 scrollbar-thin scrollbar-thumb-purple-300 scrollbar-track-transparent">
            {messages.map((message, index) => (
              <div
                key={index}
                className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'} animate-fade-in-up`}
                style={{ animationDelay: `${index * 0.1}s` }}
              >
                <div
                  className={`max-w-[70%] rounded-2xl p-4 shadow-lg transform transition-all duration-300 hover:scale-[1.02] ${
                    message.role === 'user'
                      ? 'bg-gradient-to-br from-blue-600 via-purple-600 to-pink-600 text-white shadow-purple-500/30'
                      : 'bg-white/90 backdrop-blur-md text-gray-800 border-2 border-purple-200/50 shadow-purple-200/50'
                  }`}
                >
                  {/* Avatar */}
                  <div className="flex items-start gap-3">
                    <div className={`flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center ${
                      message.role === 'user' 
                        ? 'bg-white/20' 
                        : 'bg-gradient-to-br from-purple-500 to-pink-500'
                    }`}>
                      <span className="text-lg">
                        {message.role === 'user' ? '👤' : '🤖'}
                      </span>
                    </div>
                    <div className="flex-1">
                      <div className={`prose prose-sm max-w-none ${
                        message.role === 'user' ? 'prose-invert' : ''
                      }`}>
                        <ReactMarkdown>{message.content}</ReactMarkdown>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            ))}
            
            {loading && (
              <div className="flex justify-start animate-fade-in">
                <div className="bg-white/90 backdrop-blur-md border-2 border-purple-200/50 rounded-2xl p-4 shadow-lg shadow-purple-200/50">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-full bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center">
                      <span className="text-lg">🤖</span>
                    </div>
                    <div className="flex space-x-2">
                      <div className="w-3 h-3 bg-gradient-to-r from-purple-500 to-pink-500 rounded-full animate-bounce"></div>
                      <div className="w-3 h-3 bg-gradient-to-r from-purple-500 to-pink-500 rounded-full animate-bounce" style={{ animationDelay: '0.2s' }}></div>
                      <div className="w-3 h-3 bg-gradient-to-r from-purple-500 to-pink-500 rounded-full animate-bounce" style={{ animationDelay: '0.4s' }}></div>
                    </div>
                  </div>
                </div>
              </div>
            )}
            
            <div ref={messagesEndRef} />
          </div>

          {/* Quick Questions */}
          {messages.length === 1 && !loading && (
            <div className="relative z-10 px-6 py-4 border-t-2 border-purple-200/30 bg-gradient-to-r from-purple-50/50 to-pink-50/50 backdrop-blur-sm">
              <p className="text-sm font-semibold text-purple-700 mb-3 flex items-center gap-2">
                <span>✨</span> Gợi ý câu hỏi:
              </p>
              <div className="flex flex-wrap gap-2">
                {quickQuestions.map((question, index) => (
                  <button
                    key={index}
                    onClick={() => setInput(question)}
                    className="px-4 py-2 bg-white/80 backdrop-blur-sm border-2 border-purple-300/50 rounded-xl text-sm text-gray-700 hover:border-purple-500 hover:bg-gradient-to-r hover:from-purple-50 hover:to-pink-50 hover:shadow-lg hover:shadow-purple-300/30 hover:-translate-y-0.5 transition-all duration-300 font-medium"
                  >
                    {question}
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Input Area */}
          <div className="relative z-10 p-4 border-t-2 border-purple-200/30 bg-white/70 backdrop-blur-md">
            <div className="flex gap-3">
              <textarea
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyPress={handleKeyPress}
                placeholder="💬 Nhập câu hỏi của bạn..."
                className="flex-1 px-4 py-3 border-2 border-purple-300/50 rounded-xl focus:outline-none focus:border-purple-500 focus:ring-2 focus:ring-purple-200 resize-none bg-white/80 backdrop-blur-sm transition-all duration-300 placeholder:text-gray-400"
                rows="2"
                disabled={loading}
              />
              <button
                onClick={sendMessage}
                disabled={loading || !input.trim()}
                className="px-8 py-3 bg-gradient-to-r from-purple-600 via-pink-600 to-purple-600 text-white font-bold rounded-xl shadow-xl shadow-purple-500/40 hover:-translate-y-1 hover:shadow-2xl hover:shadow-purple-500/50 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:translate-y-0 disabled:hover:shadow-xl text-lg flex items-center justify-center min-w-[80px]"
              >
                {loading ? (
                  <div className="animate-spin">⏳</div>
                ) : (
                  <span className="flex items-center gap-2">
                    <span>Gửi</span>
                    <span className="text-xl">➤</span>
                  </span>
                )}
              </button>
            </div>
          </div>
        </div>

        {/* API Key Warning */}
        {!GEMINI_API_KEY && (
          <div className="mt-4 bg-amber-50/80 backdrop-blur-md rounded-lg border-2 border-amber-300 p-4">
            <p className="text-amber-800 text-sm">
              ⚠️ <strong>Chưa cấu hình API Key:</strong> Thêm <code className="bg-amber-100 px-2 py-1 rounded">REACT_APP_GEMINI_API_KEY</code> vào file <code className="bg-amber-100 px-2 py-1 rounded">.env</code> để sử dụng tính năng AI Chat.
            </p>
          </div>
        )}
        </div>
      </div>
    </div>
  );
};

export default Chat;
