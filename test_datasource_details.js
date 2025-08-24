// 测试获取数据源详细信息
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
    console.log('🔍 测试数据源详细信息获取\n');
    
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
            
            console.log('数据源详情响应状态:', detailResponse.status);
            
            if (detailResponse.data.success) {
                const datasource = detailResponse.data.data;
                console.log('✅ 数据源详情获取成功');
                console.log('数据源详细信息:');
                console.log(`- ID: ${datasource.id}`);
                console.log(`- 名称: ${datasource.name}`);
                console.log(`- 类型: ${datasource.type}`);
                console.log(`- 主机: ${datasource.host}`);
                console.log(`- 端口: ${datasource.port}`);
                console.log(`- 数据库: ${datasource.database || '(空)'}`);
                console.log(`- 用户名: ${datasource.username || '(空)'}`);
                console.log(`- 连接URL: ${datasource.connectionUrl || '(空)'}`);
                console.log(`- SSL启用: ${datasource.sslEnabled}`);
                console.log(`- 启用状态: ${datasource.enabled}`);
                console.log('\n完整响应数据:');
                console.log(JSON.stringify(datasource, null, 2));
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