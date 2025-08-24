// 测试修复后的数据源连接功能
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
                    resolve({ status: res.statusCode, data: data });
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

async function test() {
    console.log('🔧 测试修复后的数据源连接功能\n');
    
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
            
            // 2. 测试新建模式的配置测试功能
            console.log('\n2. 测试新建模式的配置测试功能...');
            const testConfigData = {
                name: '测试数据源',
                type: 'H2',
                host: 'localhost',
                port: 8080,
                database: 'autoapi',
                username: 'sa',
                password: '',
                connectionUrl: 'jdbc:h2:mem:autoapi',
                sslEnabled: false
            };
            
            const configTestResponse = await makeRequest(`${API_BASE_URL}/api/datasources/test-config`, {
                method: 'POST',
                headers: { 
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json' 
                },
                body: JSON.stringify(testConfigData)
            });
            
            console.log('配置测试响应状态:', configTestResponse.status);
            console.log('配置测试响应数据:', JSON.stringify(configTestResponse.data, null, 2));
            
            if (configTestResponse.data.success) {
                console.log('✅ 新建模式配置测试成功!');
            } else {
                console.error('❌ 新建模式配置测试失败:', configTestResponse.data.message);
            }
            
            // 3. 测试编辑模式的连接测试功能
            console.log('\n3. 测试编辑模式的连接测试功能...');
            const editTestResponse = await makeRequest(`${API_BASE_URL}/api/datasources/1/test`, {
                method: 'POST',
                headers: { 
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json' 
                }
            });
            
            console.log('编辑模式测试响应状态:', editTestResponse.status);
            console.log('编辑模式测试响应数据:', JSON.stringify(editTestResponse.data, null, 2));
            
            if (editTestResponse.data.success) {
                console.log('✅ 编辑模式连接测试成功!');
            } else {
                console.error('❌ 编辑模式连接测试失败:', editTestResponse.data.message);
            }
            
        } else {
            console.error('❌ 登录失败:', loginResponse.data.message);
        }
    } catch (error) {
        console.error('❌ 测试异常:', error.message);
    }
}

test();