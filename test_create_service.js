// 测试创建API服务的功能
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
    console.log('🚀 测试创建API服务功能\n');
    
    try {
        // 1. 登录
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
                    console.log(`   使用数据源: ${datasource.name} (ID: ${datasource.id})`);
                    
                    // 3. 创建API服务
                    console.log('\n3. 创建测试API服务...');
                    const createServiceResponse = await makeRequest(`${API_BASE_URL}/api/services`, {
                        method: 'POST',
                        headers: { 
                            'Authorization': `Bearer ${token}`,
                            'Content-Type': 'application/json' 
                        },
                        body: JSON.stringify({
                            name: '测试服务_' + Date.now(),
                            description: '测试服务描述',
                            path: '/api/test_' + Date.now(),
                            method: 'GET',
                            dataSourceId: datasource.id,
                            sqlContent: 'SELECT * FROM users',
                            requestParams: '{}',
                            responseExample: '{}',
                            cacheEnabled: false,
                            cacheDuration: 300,
                            rateLimit: 100,
                            enabled: true
                        })
                    });
                    
                    console.log('创建服务响应状态:', createServiceResponse.status);
                    console.log('创建服务响应数据:', JSON.stringify(createServiceResponse.data, null, 2));
                    
                    if (createServiceResponse.data.success) {
                        console.log('✅ API服务创建成功!');
                        const newService = createServiceResponse.data.data;
                        console.log(`   服务ID: ${newService.id}`);
                        console.log(`   服务名称: ${newService.name}`);
                        console.log(`   服务路径: ${newService.path}`);
                    } else {
                        console.error('❌ 创建API服务失败:', createServiceResponse.data.message);
                    }
                } else {
                    console.error('❌ 没有可用的数据源');
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