// 测试数据源连接API的脚本
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
    console.log('🧪 测试数据源连接API功能\n');
    
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
            
            // 2. 获取数据源列表
            console.log('\n2. 获取数据源列表...');
            const dsResponse = await makeRequest(`${API_BASE_URL}/api/datasources`, {
                method: 'GET',
                headers: { 
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json' 
                }
            });
            
            if (dsResponse.data.success) {
                const datasources = dsResponse.data.data;
                console.log(`✅ 获取到 ${datasources.length} 个数据源`);
                
                if (datasources.length > 0) {
                    const datasource = datasources[0];
                    console.log(`使用数据源: ${datasource.name} (ID: ${datasource.id})`);
                    
                    // 3. 测试数据源连接
                    console.log('\n3. 测试数据源连接...');
                    const testResponse = await makeRequest(`${API_BASE_URL}/api/datasources/${datasource.id}/test`, {
                        method: 'POST',
                        headers: { 
                            'Authorization': `Bearer ${token}`,
                            'Content-Type': 'application/json' 
                        }
                    });
                    
                    console.log('测试连接响应状态:', testResponse.status);
                    console.log('测试连接响应头信息:');
                    console.log('测试连接响应数据:', JSON.stringify(testResponse.data, null, 2));
                    
                    if (testResponse.data.success) {
                        console.log('✅ 数据源连接测试成功!');
                        const status = testResponse.data.data;
                        console.log(`连接状态: ${status.connected ? '已连接' : '连接失败'}`);
                        if (status.responseTime) {
                            console.log(`响应时间: ${status.responseTime}ms`);
                        }
                        if (status.error) {
                            console.log(`错误信息: ${status.error}`);
                        }
                    } else {
                        console.error('❌ 数据源连接测试失败:', testResponse.data.message);
                        if (testResponse.status === 403) {
                            console.error('🚫 403 Forbidden - 可能的原因:');
                            console.error('   - JWT token无效或过期');
                            console.error('   - 用户权限不足');
                            console.error('   - CORS配置问题');
                            console.error('   - API路径配置错误');
                        }
                    }
                } else {
                    console.error('❌ 没有可用的数据源进行测试');
                }
            } else {
                console.error('❌ 获取数据源失败:', dsResponse.data.message);
            }
        } else {
            console.error('❌ 登录失败:', loginResponse.data.message);
        }
    } catch (error) {
        console.error('❌ 测试异常:', error.message);
    }
}

test();