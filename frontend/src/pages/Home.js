import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import { deviceAPI } from '../services/api';

const DEVICE_UID = process.env.REACT_APP_DEVICE_UID || 'ESP32_GARDEN_001';

const Home = () => {
  const [deviceState, setDeviceState] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDeviceState();
    const interval = setInterval(loadDeviceState, 5000); // Update every 5 seconds
    return () => clearInterval(interval);
  }, []);

  const loadDeviceState = async () => {
    try {
      const response = await deviceAPI.getDeviceState(DEVICE_UID);
      console.log('🏠 Home - Device state response:', response.data);
      if (response.data && response.data.data) {
        setDeviceState(response.data.data);
        console.log('🏠 Home - Device state set:', response.data.data);
      }
    } catch (error) {
      console.error('❌ Error loading device state:', error);
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = () => {
    if (!deviceState || !deviceState.sensors) return 'gray';
    const temp = deviceState.sensors.temperature || 0;
    const soilMoisture = deviceState.sensors.soil_moisture || 0;
    
    if (temp > 35 || soilMoisture < 30) return 'red';
    if (temp < 18 || soilMoisture < 50) return 'yellow';
    return 'green';
  };

  const getStatusText = () => {
    const color = getStatusColor();
    if (color === 'green') return 'Tốt';
    if (color === 'yellow') return 'Cần chú ý';
    return 'Cảnh báo';
  };

  return (
    <div className="min-h-screen bg-gray-50" style={{
      backgroundImage: 'url(/img/image.png)',
      backgroundSize: 'cover',
      backgroundAttachment: 'fixed',
      backgroundPosition: 'center'
    }}>
      <div className="absolute inset-0 bg-white/85 backdrop-blur-sm"></div>
      
      <div className="relative z-10">
        <Navbar />

        {/* Hero Section */}
        <div className="max-w-7xl mx-auto px-6 py-20">
          <div className="text-center mb-16">
            <h1 className="text-6xl font-bold text-gray-900 mb-4">
              Smart Garden IoT
            </h1>
            <p className="text-2xl text-gray-800 mb-8">
              Hệ thống tưới tự động thông minh với AI
            </p>
            <Link
              to="/control"
              className="inline-block px-8 py-4 bg-gradient-to-r from-green-600 to-emerald-500 text-white text-lg font-semibold rounded-lg shadow-lg shadow-green-500/30 hover:-translate-y-1 transition-all duration-300"
            >
              Bắt đầu điều khiển →
            </Link>
          </div>

          {/* Real-time Status Card */}
          {!loading && deviceState && (
            <div className="mb-12 bg-white/90 backdrop-blur-md rounded-lg border-2 border-gray-300 shadow-xl p-6">
              <div className="flex items-center justify-between mb-4">
                <h2 className="text-2xl font-bold text-gray-800">📡 Trạng thái hiện tại</h2>
                <div className={`flex items-center gap-2 px-4 py-2 rounded-lg border-2 ${
                  getStatusColor() === 'green' ? 'bg-green-100 border-green-400 text-green-800' :
                  getStatusColor() === 'yellow' ? 'bg-yellow-100 border-yellow-400 text-yellow-800' :
                  'bg-red-100 border-red-400 text-red-800'
                }`}>
                  <div className={`w-3 h-3 rounded-full ${
                    getStatusColor() === 'green' ? 'bg-green-500' :
                    getStatusColor() === 'yellow' ? 'bg-yellow-500' :
                    'bg-red-500'
                  } animate-pulse`}></div>
                  <span className="font-semibold">{getStatusText()}</span>
                </div>
              </div>

              <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                <StatusItem
                  icon="🌡️"
                  label="Nhiệt độ"
                  value={`${deviceState.sensors?.temperature ?? 'N/A'}°C`}
                />
                <StatusItem
                  icon="💧"
                  label="Độ ẩm KK"
                  value={`${deviceState.sensors?.air_humidity ?? 'N/A'}%`}
                />
                <StatusItem
                  icon="🌱"
                  label="Độ ẩm đất"
                  value={`${deviceState.sensors?.soil_moisture ?? 'N/A'}%`}
                />
                <StatusItem
                  icon="⚙️"
                  label="Máy bơm"
                  value={deviceState.pump_state === 'ON' ? 'BẬT' : 'TẮT'}
                  valueColor={deviceState.pump_state === 'ON' ? 'text-green-600' : 'text-gray-600'}
                />
              </div>

              <div className="mt-6 flex gap-3 justify-center">
                <Link
                  to="/control"
                  className="px-6 py-3 bg-gradient-to-r from-blue-600 to-cyan-500 text-white font-semibold rounded-lg shadow-lg shadow-blue-500/30 hover:-translate-y-1 transition-all duration-300"
                >
                  Điều khiển
                </Link>
                <Link
                  to="/dashboard"
                  className="px-6 py-3 bg-gradient-to-r from-purple-600 to-pink-500 text-white font-semibold rounded-lg shadow-lg shadow-purple-500/30 hover:-translate-y-1 transition-all duration-300"
                >
                  Thống kê
                </Link>
                <Link
                  to="/chat"
                  className="px-6 py-3 bg-gradient-to-r from-amber-600 to-orange-500 text-white font-semibold rounded-lg shadow-lg shadow-amber-500/30 hover:-translate-y-1 transition-all duration-300"
                >
                  AI Chat
                </Link>
              </div>
            </div>
          )}

          {/* Feature Cards */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <FeatureCard
              icon="🌱"
              title="Giám sát thời gian thực"
              description="Theo dõi nhiệt độ, độ ẩm không khí và độ ẩm đất 24/7"
              link="/control"
            />
            <FeatureCard
              icon="💧"
              title="Tưới tự động"
              description="Hệ thống tưới thông minh dựa trên dữ liệu cảm biến"
              link="/control"
            />
            <FeatureCard
              icon="🤖"
              title="AI hỗ trợ"
              description="Trợ lý AI thông minh giúp chăm sóc vườn hiệu quả hơn"
              link="/chat"
            />
          </div>
        </div>
      </div>
    </div>
  );
};

const StatusItem = ({ icon, label, value, valueColor = 'text-gray-900' }) => {
  return (
    <div className="text-center">
      <div className="text-3xl mb-2">{icon}</div>
      <div className="text-sm text-gray-600 mb-1">{label}</div>
      <div className={`text-xl font-bold ${valueColor}`}>{value}</div>
    </div>
  );
};

const FeatureCard = ({ icon, title, description, link }) => {
  return (
    <Link to={link} className="block">
      <div className="bg-white/80 backdrop-blur-md rounded-lg border-2 border-gray-300 shadow-xl hover:-translate-y-1 transition-all duration-300">
        <div className="p-6 border-t-4 border-green-500">
          <div className="text-5xl mb-4">{icon}</div>
          <h3 className="text-xl font-bold text-gray-800 mb-2">{title}</h3>
          <p className="text-gray-600">{description}</p>
        </div>
      </div>
    </Link>
  );
};

export default Home;
