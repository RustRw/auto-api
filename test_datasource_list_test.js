// 测试数据源列表页面的测试连接功能
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

async function test() {
    console.log('🧪 测试数据源列表的测试连接功能\n');
    
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
            
            // 2. 获取数据源详情
            console.log('\n2. 获取数据源详情...');
            const detailResponse = await makeRequest(`${API_BASE_URL}/api/datasources/1`, {
                method: 'GET',
                headers: { 
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json' 
                }
            });
            
            if (detailResponse.data.success) {
                const datasource = detailResponse.data.data;
                console.log('✅ 数据源详情获取成功');
                
                // 3. 模拟列表页面的测试连接功能（新修复的API）
                console.log('\n3. 测试列表页面的连接测试功能...');
                const testData = {
                    name: datasource.name,
                    type: datasource.type,
                    host: datasource.host,
                    port: datasource.port,
                    database: datasource.database,
                    username: datasource.username,
                    password: '',
                    connectionUrl: datasource.connectionUrl || `jdbc:h2:mem:${datasource.database}`,
                    sslEnabled: datasource.sslEnabled
                };
                
                console.log('测试数据:', testData);
                
                const listTestResponse = await makeRequest(`${API_BASE_URL}/api/datasources/test-config`, {
                    method: 'POST',
                    headers: { 
                        'Authorization': `Bearer ${token}`,
                        'Content-Type': 'application/json' 
                    },
                    body: JSON.stringify(testData)
                });
                
                console.log('列表测试响应状态:', listTestResponse.status);
                
                if (listTestResponse.status === 200 && listTestResponse.data) {
                    if (listTestResponse.data.success) {
                        console.log('✅ 列表页面连接测试成功!');
                        const status = listTestResponse.data.data;
                        console.log(`连接状态: ${status.connected ? '已连接' : '连接失败'}`);
                        if (status.responseTime !== undefined) {
                            console.log(`响应时间: ${status.responseTime}ms`);
                        }
                    } else {
                        console.error('❌ 列表页面连接测试失败:', listTestResponse.data.message);
                    }
                } else {
                    console.error('❌ 列表页面连接测试请求失败');
                    console.error('错误详情:', listTestResponse.error || '未知错误');
                    console.error('响应内容:', listTestResponse.data);
                }
                
                // 4. 测试编辑页面的连接测试功能（已保存数据源）
                console.log('\n4. 测试编辑页面的连接测试功能...');
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
                    } else {
                        console.error('❌ 编辑页面连接测试失败:', editTestResponse.data.message);
                    }
                } else {
                    console.error('❌ 编辑页面连接测试请求失败');
                    console.error('错误详情:', editTestResponse.error || '未知错误');
                }
                
            } else {
                console.error('❌ 获取数据源详情失败:', detailResponse.data.message);
            }
            
        } else {
            console.error('❌ 登录失败:', loginResponse.data.message);
        }
    } catch (error) {
        console.error('❌ 测试异常:', error.message);
    }
}

test();