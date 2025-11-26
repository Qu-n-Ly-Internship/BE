// Debug script để kiểm tra API allowance với token thật
const axios = require('axios');

const BASE_URL = 'http://localhost:8090/api';

// Test function với token thật
async function testWithRealToken() {
    try {
        console.log('🧪 Testing Allowance API with real token...');
        
        // Test 1: Đăng nhập để lấy token
        console.log('\n1. Testing login to get token');
        try {
            const loginResponse = await axios.post(`${BASE_URL}/auth/login`, {
                email: 'test@example.com', // Thay bằng email thật
                password: 'password123'     // Thay bằng password thật
            });
            console.log('✅ Login successful');
            console.log('🔑 Token:', loginResponse.data.token?.substring(0, 20) + '...');
            
            const token = loginResponse.data.token;
            
            // Test 2: Sử dụng token để gọi API my-history
            console.log('\n2. Testing GET /api/allowances/my-history with real token');
            try {
                const response = await axios.get(`${BASE_URL}/allowances/my-history`, {
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                });
                console.log('✅ GET /api/allowances/my-history - Status:', response.status);
                console.log('📊 Data:', response.data);
            } catch (error) {
                console.log('❌ GET /api/allowances/my-history - Error:', error.response?.status);
                console.log('📝 Error details:', error.response?.data);
            }
            
        } catch (loginError) {
            console.log('❌ Login failed:', loginError.response?.status, loginError.response?.data?.message);
            
            // Test với token giả để xem lỗi
            console.log('\n3. Testing with fake token to see error details');
            try {
                const response = await axios.get(`${BASE_URL}/allowances/my-history`, {
                    headers: {
                        'Authorization': 'Bearer fake-token-123'
                    }
                });
            } catch (error) {
                console.log('❌ Error with fake token:', error.response?.status);
                console.log('📝 Error details:', error.response?.data);
            }
        }

    } catch (error) {
        console.error('❌ Test failed:', error.message);
    }
}

// Run test
testWithRealToken();

