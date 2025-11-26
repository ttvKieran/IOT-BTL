import React, { useState, useEffect, useCallback } from 'react';
import Navbar from '../components/Navbar';
import { Line } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  TimeScale
} from 'chart.js';
import 'chartjs-adapter-date-fns';
import { format, subHours, subDays } from 'date-fns';
import { deviceAPI } from '../services/api';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  Title,
  Tooltip,
  Legend,
  TimeScale
);

const DEVICE_UID = process.env.REACT_APP_DEVICE_UID || 'ESP32_GARDEN_001';

const Dashboard = () => {
  const [timeRange, setTimeRange] = useState('24h');
  const [historyData, setHistoryData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const loadHistoryData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      const now = Date.now();
      let from;
      
      switch (timeRange) {
        case '1h':
          from = subHours(now, 1).getTime();
          break;
        case '6h':
          from = subHours(now, 6).getTime();
          break;
        case '24h':
          from = subHours(now, 24).getTime();
          break;
        case '7d':
          from = subDays(now, 7).getTime();
          break;
        default:
          from = subHours(now, 24).getTime();
      }

      const response = await deviceAPI.getHistory(DEVICE_UID, from, now);
      console.log('📊 History data:', response.data);

      if (response.data && response.data.data) {
        setHistoryData(response.data.data);
      }
    } catch (err) {
      console.error('❌ Error loading history:', err);
      setError('Không thể tải dữ liệu lịch sử');
    } finally {
      setLoading(false);
    }
  }, [timeRange]);

  useEffect(() => {
    loadHistoryData();
    const interval = setInterval(loadHistoryData, 60000); // Refresh every minute
    return () => clearInterval(interval);
  }, [loadHistoryData]);

  const prepareChartData = (dataKey, label, color) => {
    const sortedData = [...historyData].sort((a, b) => {
      const timeA = new Date(a.log_time || a.timestamp).getTime();
      const timeB = new Date(b.log_time || b.timestamp).getTime();
      return timeA - timeB;
    });
    
    return {
      labels: sortedData.map(d => new Date(d.log_time || d.timestamp)),
      datasets: [
        {
          label: label,
          data: sortedData.map(d => d[dataKey]),
          borderColor: color,
          backgroundColor: `${color}33`,
          borderWidth: 2,
          tension: 0.4,
          pointRadius: 2,
          pointHoverRadius: 5,
        }
      ]
    };
  };

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    interaction: {
      mode: 'index',
      intersect: false,
    },
    plugins: {
      legend: {
        position: 'top',
      },
      tooltip: {
        callbacks: {
          title: (context) => {
            return format(new Date(context[0].parsed.x), 'dd/MM/yyyy HH:mm:ss');
          }
        }
      }
    },
    scales: {
      x: {
        type: 'time',
        time: {
          unit: timeRange === '7d' ? 'day' : timeRange === '24h' ? 'hour' : 'minute',
          displayFormats: {
            minute: 'HH:mm',
            hour: 'HH:mm',
            day: 'dd/MM'
          }
        },
        title: {
          display: true,
          text: 'Thời gian'
        }
      },
      y: {
        beginAtZero: true,
        title: {
          display: true,
          text: 'Giá trị'
        }
      }
    }
  };

  const temperatureOptions = {
    ...chartOptions,
    scales: {
      ...chartOptions.scales,
      y: {
        ...chartOptions.scales.y,
        title: {
          display: true,
          text: 'Nhiệt độ (°C)'
        }
      }
    }
  };

  const humidityOptions = {
    ...chartOptions,
    scales: {
      ...chartOptions.scales,
      y: {
        ...chartOptions.scales.y,
        max: 100,
        title: {
          display: true,
          text: 'Độ ẩm (%)'
        }
      }
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-purple-50 to-pink-50" style={{
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
          <h1 className="text-4xl font-bold text-gray-800 mb-4">Thống kê dữ liệu</h1>
          <p className="text-gray-600">Biểu đồ lịch sử dữ liệu cảm biến theo thời gian</p>
        </div>

        {/* Time Range Selector */}
        <div className="mb-6 flex gap-2 flex-wrap">
          {['1h', '6h', '24h', '7d'].map((range) => (
            <button
              key={range}
              onClick={() => setTimeRange(range)}
              className={`px-4 py-2 rounded-lg border-2 font-semibold transition-all duration-300 ${
                timeRange === range
                  ? 'bg-gradient-to-r from-blue-600 to-purple-600 text-white border-transparent shadow-lg'
                  : 'bg-white/80 backdrop-blur-md border-gray-300 text-gray-700 hover:border-blue-400 hover:shadow-md'
              }`}
            >
              {range === '1h' && '1 giờ'}
              {range === '6h' && '6 giờ'}
              {range === '24h' && '24 giờ'}
              {range === '7d' && '7 ngày'}
            </button>
          ))}
        </div>

        {/* Loading State */}
        {loading && historyData.length === 0 && (
          <div className="bg-white/80 backdrop-blur-md rounded-lg border-2 border-gray-300 shadow-xl p-12 text-center">
            <div className="animate-spin text-6xl mb-4">⏳</div>
            <p className="text-gray-600">Đang tải dữ liệu...</p>
          </div>
        )}

        {/* Error State */}
        {error && (
          <div className="bg-red-50/80 backdrop-blur-md rounded-lg border-2 border-red-300 shadow-xl p-6 mb-6">
            <p className="text-red-600">❌ {error}</p>
          </div>
        )}

        {/* Charts */}
        {!loading && historyData.length > 0 && (
          <div className="space-y-6">
            {/* Temperature Chart */}
            <div className="bg-white/80 backdrop-blur-md rounded-lg border-2 border-gray-300 shadow-xl p-6">
              <h2 className="text-2xl font-bold text-gray-800 mb-4">🌡️ Nhiệt độ</h2>
              <div style={{ height: '300px' }}>
                <Line 
                  data={prepareChartData('temperature', 'Nhiệt độ (°C)', 'rgb(239, 68, 68)')}
                  options={temperatureOptions}
                />
              </div>
            </div>

            {/* Air Humidity Chart */}
            <div className="bg-white/80 backdrop-blur-md rounded-lg border-2 border-gray-300 shadow-xl p-6">
              <h2 className="text-2xl font-bold text-gray-800 mb-4">💧 Độ ẩm không khí</h2>
              <div style={{ height: '300px' }}>
                <Line 
                  data={prepareChartData('air_humidity', 'Độ ẩm không khí (%)', 'rgb(59, 130, 246)')}
                  options={humidityOptions}
                />
              </div>
            </div>

            {/* Soil Moisture Chart */}
            <div className="bg-white/80 backdrop-blur-md rounded-lg border-2 border-gray-300 shadow-xl p-6">
              <h2 className="text-2xl font-bold text-gray-800 mb-4">🌱 Độ ẩm đất</h2>
              <div style={{ height: '300px' }}>
                <Line 
                  data={prepareChartData('soil_moisture', 'Độ ẩm đất (%)', 'rgb(34, 197, 94)')}
                  options={humidityOptions}
                />
              </div>
            </div>
          </div>
        )}

        {/* No Data State */}
        {!loading && historyData.length === 0 && !error && (
          <div className="bg-white/80 backdrop-blur-md rounded-lg border-2 border-gray-300 shadow-xl p-12 text-center">
            <div className="text-6xl mb-4">📭</div>
            <h2 className="text-2xl font-bold text-gray-700 mb-2">Chưa có dữ liệu</h2>
            <p className="text-gray-600">Không tìm thấy dữ liệu lịch sử trong khoảng thời gian này</p>
          </div>
        )}
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
