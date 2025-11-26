import React, { useState, useEffect, useCallback } from 'react';
import Navbar from '../components/Navbar';
import { deviceAPI } from '../services/api';
import wsService from '../services/websocket';

const DEVICE_UID = process.env.REACT_APP_DEVICE_UID || 'ESP32_GARDEN_001';

const Control = () => {
  const [deviceState, setDeviceState] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [connectionStatus, setConnectionStatus] = useState('CONNECTING');
  const [pumpSwitchLoading, setPumpSwitchLoading] = useState(false);
  const [thresholds, setThresholds] = useState({ soilMoistureThreshold: 30, pumpDuration: 5 });
  const [thresholdLoading, setThresholdLoading] = useState(false);

  // Load current device state
  const loadDeviceState = useCallback(async () => {
    try {
      const response = await deviceAPI.getDeviceState(DEVICE_UID);
      setDeviceState(response.data.data);
      setConnectionStatus('ONLINE');
    } catch (error) {
      console.error('Error loading device state:', error);
      setConnectionStatus('OFFLINE');
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Load thresholds
  const loadThresholds = useCallback(async () => {
    try {
      const response = await deviceAPI.getThresholds(DEVICE_UID);
      console.log('📊 Thresholds API response:', response.data);
      
      if (response.data.data) {
        const data = response.data.data;
        setThresholds({
          soilMoistureThreshold: data.minSoilMoisture ?? 30,
          pumpDuration: data.maxPumpDurationSeconds ?? 5
        });
        console.log('✅ Loaded thresholds:', {
          minSoilMoisture: data.minSoilMoisture,
          maxPumpDurationSeconds: data.maxPumpDurationSeconds
        });
      }
    } catch (error) {
      console.error('❌ Error loading thresholds:', error);
      console.error('❌ Error details:', error.response?.data);
    }
  }, []);

  // Connect WebSocket
  useEffect(() => {
    loadDeviceState();
    loadThresholds();

    wsService.connect(
      () => {
        console.log('✅ WebSocket connected');
        setConnectionStatus('ONLINE');

        // Subscribe to device-specific topic
        wsService.subscribe(`/topic/device/${DEVICE_UID}`, (data) => {
          console.log('📩 Received update:', data);
          setDeviceState(data);
        });

        // Subscribe to fallback topic
        wsService.subscribe('/topic/all-devices', (data) => {
          if (data.device_uid === DEVICE_UID) {
            console.log('📩 Fallback update:', data);
            setDeviceState(data);
          }
        });
      },
      (error) => {
        console.error('❌ WebSocket error:', error);
        setConnectionStatus('OFFLINE');
      }
    );

    // Polling fallback (every 3s)
    const pollingInterval = setInterval(() => {
      loadDeviceState();
      console.log('🔄 Polling fallback...');
    }, 3000);

    return () => {
      wsService.disconnect();
      clearInterval(pollingInterval);
    };
  }, [loadDeviceState, loadThresholds]);

  // Toggle pump
  const handlePumpToggle = async () => {
    if (!deviceState || pumpSwitchLoading) return;

    const newState = deviceState.pump_state !== 'ON';
    console.log(`🎯 Current pump state: ${deviceState.pump_state}, toggling to: ${newState ? 'ON' : 'OFF'}`);
    setPumpSwitchLoading(true);

    try {
      const response = await deviceAPI.sendPumpCommand(DEVICE_UID, newState);
      console.log(`✅ Pump ${newState ? 'ON' : 'OFF'} command sent, response:`, response.data);
      
      // Don't do optimistic update - wait for WebSocket/polling to update
      // The backend/ESP32 needs time to process the command
    } catch (error) {
      console.error('❌ Error toggling pump:', error);
      alert('Lỗi khi điều khiển máy bơm!');
    } finally {
      // Keep loading state for 1 second to give backend time to process
      setTimeout(() => setPumpSwitchLoading(false), 1000);
    }
  };

  // Save thresholds
  const handleSaveThresholds = async () => {
    setThresholdLoading(true);
    try {
      await deviceAPI.setThresholds(
        DEVICE_UID,
        thresholds.soilMoistureThreshold,
        thresholds.pumpDuration
      );
      console.log('✅ Thresholds saved:', thresholds);
      alert('Lưu ngưỡng thành công!');
    } catch (error) {
      console.error('❌ Error saving thresholds:', error);
      alert('Lỗi khi lưu ngưỡng!');
    } finally {
      setThresholdLoading(false);
    }
  };

  // Change control mode
  const handleModeChange = async (mode) => {
    if (!deviceState) return;

    console.log(`🎯 Attempting to change mode to: ${mode}`);

    try {
      const autoOff = mode === 'AUTO';
      console.log(`📡 Calling API with autoOff=${autoOff}`);
      const response = await deviceAPI.setControlMode(DEVICE_UID, autoOff);
      console.log(`✅ Mode change API response:`, response.data);
      
      // Optimistic update - UI updates immediately
      setDeviceState(prev => ({
        ...prev,
        control_mode: mode
      }));
      console.log(`🔄 UI updated to ${mode} mode`);
    } catch (error) {
      console.error('❌ Error changing mode:', error);
      console.error('❌ Error details:', error.response?.data);
      alert('Lỗi khi thay đổi chế độ: ' + (error.response?.data?.message || error.message));
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50">
        <Navbar />
        <div className="flex items-center justify-center h-screen">
          <div className="text-center">
            <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-green-600 mx-auto"></div>
            <p className="mt-4 text-gray-600">Đang tải...</p>
          </div>
        </div>
      </div>
    );
  }

  const sensors = deviceState?.sensors || {};
  const temperature = sensors.temperature || 0;
  const airHumidity = sensors.air_humidity || sensors.airHumidity || 0;
  const soilMoisture = sensors.soil_moisture || sensors.soilMoisture || 0;
  const pumpState = deviceState?.pump_state || deviceState?.pumpState || 'OFF';
  const controlMode = deviceState?.control_mode || deviceState?.controlMode || 'MANUAL';

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

        <div className="max-w-7xl mx-auto px-6 py-8">
          {/* Header */}
          <div className="mb-8">
            <h1 className="text-4xl font-bold text-gray-800 mb-2">Điều khiển thiết bị</h1>
            <p className="text-gray-600">Quản lý và giám sát hệ thống tưới tự động</p>
          </div>

          {/* Status Indicator */}
          <div className="mb-6 flex items-center space-x-3">
            <div className={`w-3 h-3 rounded-full ${connectionStatus === 'ONLINE' ? 'bg-green-500 animate-pulse' : 'bg-red-500'}`}></div>
            <span className="font-medium text-gray-700">
              Trạng thái: <span className={connectionStatus === 'ONLINE' ? 'text-green-600' : 'text-red-600'}>
                {connectionStatus === 'ONLINE' ? 'Trực tuyến' : 'Ngoại tuyến'}
              </span>
            </span>
          </div>

          {/* Main Content */}
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
            {/* Control Panel (Sidebar) */}
            <div className="lg:col-span-1">
              <div className="bg-white/80 backdrop-blur-md rounded-lg border-2 border-gray-300 shadow-xl sticky top-24">
                <div className="p-6 border-t-4 border-green-500">
                  <h2 className="text-2xl font-bold text-gray-800 mb-6">Bảng điều khiển</h2>

                  {/* Pump Switch */}
                  <div className="mb-8">
                    <label className="flex items-center justify-between p-4 bg-gradient-to-r from-blue-50 to-cyan-50 rounded-lg border-2 border-blue-200 cursor-pointer hover:-translate-y-1 transition-all duration-300">
                      <div>
                        <div className="text-lg font-semibold text-gray-800">Máy bơm nước</div>
                        <div className="text-sm text-gray-600">
                          Trạng thái: <span className={`font-bold ${pumpState === 'ON' ? 'text-green-600' : 'text-gray-500'}`}>
                            {pumpState === 'ON' ? 'BẬT' : 'TẮT'}
                          </span>
                        </div>
                      </div>
                      <div className="relative">
                        <input
                          type="checkbox"
                          className="sr-only peer"
                          checked={pumpState === 'ON'}
                          onChange={handlePumpToggle}
                          disabled={pumpSwitchLoading || controlMode === 'AUTO'}
                        />
                        <div className="w-14 h-8 bg-gray-300 rounded-full peer-checked:bg-green-500 transition-all duration-300 peer-disabled:opacity-50"></div>
                        <div className="absolute left-1 top-1 w-6 h-6 bg-white rounded-full shadow-md transition-all duration-300 peer-checked:translate-x-6"></div>
                      </div>
                    </label>
                    {controlMode === 'AUTO' && (
                      <p className="text-xs text-amber-600 mt-2">⚠️ Chế độ tự động đang bật. Tắt để điều khiển thủ công.</p>
                    )}
                  </div>

                  {/* Mode Buttons */}
                  <div>
                    <h3 className="text-sm font-semibold text-gray-600 mb-3">Chế độ điều khiển</h3>
                    <div className="grid grid-cols-2 gap-3">
                      <button
                        onClick={() => handleModeChange('MANUAL')}
                        className={`py-3 px-4 rounded-lg font-semibold transition-all duration-300 border-2 ${
                          controlMode === 'MANUAL'
                            ? 'bg-gradient-to-r from-blue-600 to-cyan-500 text-white border-blue-600 shadow-lg shadow-blue-500/30'
                            : 'bg-white text-gray-700 border-gray-300 hover:border-blue-400'
                        }`}
                      >
                        Thủ công
                      </button>
                      <button
                        onClick={() => handleModeChange('AUTO')}
                        className={`py-3 px-4 rounded-lg font-semibold transition-all duration-300 border-2 ${
                          controlMode === 'AUTO'
                            ? 'bg-gradient-to-r from-green-600 to-emerald-500 text-white border-green-600 shadow-lg shadow-green-500/30'
                            : 'bg-white text-gray-700 border-gray-300 hover:border-green-400'
                        }`}
                      >
                        Tự động
                      </button>
                    </div>
                  </div>

                  {/* Threshold Settings - Only show in MANUAL mode */}
                  {controlMode === 'MANUAL' && (
                    <div className="mt-6 p-4 bg-gradient-to-r from-amber-50 to-orange-50 rounded-lg border-2 border-amber-200">
                      <h3 className="text-sm font-semibold text-gray-800 mb-4">⚙️ Cài đặt ngưỡng</h3>
                      
                      {/* Soil Moisture Threshold */}
                      <div className="mb-4">
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          Ngưỡng độ ẩm đất (%)
                        </label>
                        <input
                          type="number"
                          min="0"
                          max="100"
                          value={thresholds.soilMoistureThreshold}
                          onChange={(e) => setThresholds(prev => ({
                            ...prev,
                            soilMoistureThreshold: parseInt(e.target.value) || 0
                          }))}
                          className="w-full px-3 py-2 border-2 border-gray-300 rounded-lg focus:border-amber-500 focus:outline-none"
                        />
                        <p className="text-xs text-gray-500 mt-1">Bơm sẽ bật khi độ ẩm đất {'<'} ngưỡng này</p>
                      </div>

                      {/* Pump Duration */}
                      <div className="mb-4">
                        <label className="block text-sm font-medium text-gray-700 mb-2">
                          Thời gian bật bơm (giây)
                        </label>
                        <input
                          type="number"
                          min="1"
                          max="3600"
                          value={thresholds.pumpDuration}
                          onChange={(e) => setThresholds(prev => ({
                            ...prev,
                            pumpDuration: parseInt(e.target.value) || 1
                          }))}
                          className="w-full px-3 py-2 border-2 border-gray-300 rounded-lg focus:border-amber-500 focus:outline-none"
                        />
                        <p className="text-xs text-gray-500 mt-1">Thời gian bơm chạy mỗi lần</p>
                      </div>

                      {/* Save Button */}
                      <button
                        onClick={handleSaveThresholds}
                        disabled={thresholdLoading}
                        className="w-full py-2 px-4 bg-gradient-to-r from-amber-600 to-orange-500 text-white font-semibold rounded-lg shadow-lg shadow-amber-500/30 hover:-translate-y-0.5 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
                      >
                        {thresholdLoading ? 'Đang lưu...' : '💾 Lưu cài đặt'}
                      </button>
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Sensor Cards */}
            <div className="lg:col-span-2">
              <h2 className="text-2xl font-bold text-gray-800 mb-6">Thông số cảm biến</h2>
              
              {/* First row - 2 cards */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-6">
                <SensorCard
                  icon="🌡️"
                  title="Nhiệt độ"
                  value={temperature.toFixed(1)}
                  unit="°C"
                  gradient="from-red-400 to-red-600"
                  bgGradient="from-red-50 to-orange-50"
                  borderColor="border-red-500"
                  shadowColor="shadow-red-500/30"
                />
                <SensorCard
                  icon="💧"
                  title="Độ ẩm không khí"
                  value={airHumidity.toFixed(1)}
                  unit="%"
                  gradient="from-blue-400 to-blue-600"
                  bgGradient="from-blue-50 to-cyan-50"
                  borderColor="border-blue-500"
                  shadowColor="shadow-blue-500/30"
                />
              </div>

              {/* Second row - 1 centered card */}
              <div className="flex justify-center">
                <div className="w-full md:w-1/2 md:px-3">
                  <SensorCard
                    icon="🌿"
                    title="Độ ẩm đất"
                    value={soilMoisture.toFixed(1)}
                    unit="%"
                    gradient="from-green-400 to-green-600"
                    bgGradient="from-green-50 to-emerald-50"
                    borderColor="border-green-500"
                    shadowColor="shadow-green-500/30"
                  />
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

const SensorCard = ({ icon, title, value, unit, gradient, bgGradient, borderColor, shadowColor }) => {
  return (
    <div className={`bg-gradient-to-br ${bgGradient} rounded-lg border-2 border-gray-300 shadow-xl ${shadowColor} hover:-translate-y-1 transition-all duration-300`}>
      <div className={`p-6 border-t-4 ${borderColor}`}>
        <div className="flex items-center justify-between mb-4">
          <div className="text-4xl">{icon}</div>
          <div className="text-right">
            <div className={`text-4xl font-bold bg-gradient-to-r ${gradient} bg-clip-text text-transparent`}>
              {value}<span className="text-2xl">{unit}</span>
            </div>
          </div>
        </div>
        <div className="text-sm font-semibold text-gray-600 uppercase tracking-wide">{title}</div>
      </div>
    </div>
  );
};

export default Control;
