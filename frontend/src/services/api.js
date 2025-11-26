import axios from 'axios';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

const apiClient = axios.create({
  baseURL: `${API_URL}/api/v1`,
  headers: {
    'Content-Type': 'application/json',
  },
});

export const deviceAPI = {
  // Get all devices
  getAllDevices: () => apiClient.get('/devices'),

  // Get device state
  getDeviceState: (deviceUid) => apiClient.get(`/devices/${deviceUid}/state`),

  // Send pump command
  sendPumpCommand: (deviceUid, state) => 
    apiClient.post(`/devices/${deviceUid}/command`, {
      action: 'CONTROL_PUMP',
      payload: {
        state: state ? 'ON' : 'OFF'
      }
    }),

  // Set control mode
  setControlMode: (deviceUid, autoOff) => 
    apiClient.post(`/devices/${deviceUid}/auto-off`, null, {
      params: { autoOff }
    }),

  // Get historical data
  getHistory: (deviceUid, from, to) => 
    apiClient.get(`/devices/${deviceUid}/history`, {
      params: { from, to }
    }),

  // Get device thresholds
  getThresholds: (deviceUid) => 
    apiClient.get(`/thresholds/${deviceUid}`),

  // Set device thresholds
  setThresholds: (deviceUid, minSoilMoisture, maxPumpDurationSeconds) => 
    apiClient.post('/thresholds', {
      deviceUid,
      minSoilMoisture,
      maxPumpDurationSeconds,
      isActive: true
    }),
};

export default apiClient;
