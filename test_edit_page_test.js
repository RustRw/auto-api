// 测试编辑页面的测试连接功能
const https = require('https');
const http = require('http');

const API_BASE_URL = 'http://localhost:8080';

async function makeRequest(url, options = {}) {
    return new Promise((resolve, reject) => {
        const client = url.startsWith('https') ? https : http;
        const req = client.request(url, options, (res) => {
            let data = '';
            res.on('data', chunk => data += chunk);
            res.on('end', () => {
                try {
                    const result = JSON.parse(data);
                    resolve({ status: res.statusCode, data: result });
                } catch (e) {
                    resolve({ status: res.statusCode, data: data, error: 'JSON解析失败: ' + e.message });
                }
            });
        });
        
        req.on('error', reject);
        
        if (options.body) {
            req.write(options.body);
        }
        
        req.end();
    });
}

async function testEditPageConnection() {
    console.log('🧪 测试编辑页面的测试连接功能\n');
    
    try {
        // 1. 登录获取token
        console.log('1. 正在登录...');
        const loginResponse = await makeRequest(`${API_BASE_URL}/api/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: 'admin', password: 'admin123' })
        });
        
        if (loginResponse.data.success) {
            const token = loginResponse.data.data.token;
            console.log('✅ 登录成功');
            
            // 2. 模拟编辑页面的测试连接（已保存数据源）
            console.log('\n2. 测试编辑页面的连接测试功能（使用数据源ID 1）...');
            const editTestResponse = await makeRequest(`${API_BASE_URL}/api/datasources/1/test`, {
                method: 'POST',
                headers: { 
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json' 
                }
            });
            
            console.log('编辑测试响应状态:', editTestResponse.status);
            
            if (editTestResponse.status === 200 && editTestResponse.data) {
                if (editTestResponse.data.success) {
                    console.log('✅ 编辑页面连接测试成功!');
                    const status = editTestResponse.data.data;
                    console.log(`连接状态: ${status.connected ? '已连接' : '连接失败'}`);
                    if (status.responseTime !== undefined) {
                        console.log(`响应时间: ${status.responseTime}ms`);
                    }
                } else {
                    console.error('❌ 编辑页面连接测试失败:', editTestResponse.data.message);
                }
            } else {
                console.error('❌ 编辑页面连接测试请求失败');
                console.error('错误详情:', editTestResponse.error || '未知错误');
                console.error('响应内容:', editTestResponse.data);
            }
            
            // 3. 模拟可能的错误情况 - 直接调用test-config (这应该会导致验证错误)
            console.log('\n3. 测试可能的错误情况 - 调用test-config接口但缺少name字段...');
            const errorTestData = {
                // 故意不包含name字段
                type: "H2",
                host: "localhost",
                port: 8080,
                database: "autoapi",
                username: "SA",
                password: "",
                connectionUrl: "jdbc:h2:mem:autoapi",
                sslEnabled: false
            };
            
            console.log('错误测试数据:', errorTestData);
            
            const errorTestResponse = await makeRequest(`${API_BASE_URL}/api/datasources/test-config`, {
                method: 'POST',
                headers: { 
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json' 
                },
                body: JSON.stringify(errorTestData)
            });
            
            console.log('错误测试响应状态:', errorTestResponse.status);
            if (errorTestResponse.status !== 200) {
                console.log('❌ 如预期，缺少name字段的请求失败了');
                console.log('错误响应:', errorTestResponse.data);
            }
            
        } else {
            console.error('❌ 登录失败:', loginResponse.data.message);
        }
    } catch (error) {
        console.error('❌ 测试异常:', error.message);
    }
}

testEditPageConnection();