// Test script cuối cùng để kiểm tra API allowance
const axios = require('axios');

const BASE_URL = 'http://localhost:8090/api';

// Test function
async function testAllowanceAPI() {
    try {
        console.log('🧪 Testing Allowance API...');
        
        // Test 1: Lấy danh sách phụ cấp (không cần auth)
        console.log('\n1. Testing GET /api/allowances');
        try {
            const response = await axios.get(`${BASE_URL}/allowances`);
            console.log('✅ GET /api/allowances - Status:', response.status);
            console.log('📊 Data count:', response.data.data?.length || 0);
        } catch (error) {
            console.log('❌ GET /api/allowances - Error:', error.response?.status, error.response?.data?.message);
        }

        // Test 2: Test với token giả để xem lỗi chi tiết
        console.log('\n2. Testing GET /api/allowances/my-history with fake token');
        try {
            const response = await axios.get(`${BASE_URL}/allowances/my-history`, {
                headers: {
                    'Authorization': 'Bearer fake-token-123'
                }
            });
            console.log('✅ GET /api/allowances/my-history - Status:', response.status);
        } catch (error) {
            console.log('❌ GET /api/allowances/my-history - Error:', error.response?.status);
            console.log('📝 Error details:', JSON.stringify(error.response?.data, null, 2));
        }

        // Test 3: Test không có token
        console.log('\n3. Testing GET /api/allowances/my-history without token');
        try {
            const response = await axios.get(`${BASE_URL}/allowances/my-history`);
            console.log('✅ GET /api/allowances/my-history - Status:', response.status);
        } catch (error) {
            console.log('❌ GET /api/allowances/my-history - Error:', error.response?.status);
            console.log('📝 Error details:', JSON.stringify(error.response?.data, null, 2));
        }

        // Test 4: Test với token rỗng
        console.log('\n4. Testing GET /api/allowances/my-history with empty token');
        try {
            const response = await axios.get(`${BASE_URL}/allowances/my-history`, {
                headers: {
                    'Authorization': 'Bearer '
                }
            });
            console.log('✅ GET /api/allowances/my-history - Status:', response.status);
        } catch (error) {
            console.log('❌ GET /api/allowances/my-history - Error:', error.response?.status);
            console.log('📝 Error details:', JSON.stringify(error.response?.data, null, 2));
        }

    } catch (error) {
        console.error('❌ Test failed:', error.message);
    }
}

// Run test
testAllowanceAPI();

