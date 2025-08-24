// 测试API_SERVICES表的数据
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

async function testApiServicesData() {
    console.log('🧪 测试API_SERVICES表数据\n');
    
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
            
            // 2. 获取API_SERVICES表结构
            console.log('\n2. 获取API_SERVICES表结构...');
            const structureResponse = await makeRequest(`${API_BASE_URL}/api/datasources/1/databases/autoapi/tables/API_SERVICES/structure`, {
                method: 'GET',
                headers: { 
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json' 
                }
            });
            
            if (structureResponse.data.success) {
                console.log('✅ 获取表结构成功');
                console.log('字段数量:', structureResponse.data.data.length);
                console.log('主要字段:', structureResponse.data.data.slice(0, 5).map(col => `${col.name} (${col.type})`));
            }
            
            // 3. 获取API_SERVICES示例数据
            console.log('\n3. 获取API_SERVICES示例数据...');
            const sampleResponse = await makeRequest(`${API_BASE_URL}/api/datasources/1/databases/autoapi/tables/API_SERVICES/sample-data?limit=5`, {
                method: 'GET',
                headers: { 
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json' 
                }
            });
            
            if (sampleResponse.data.success) {
                const sampleData = sampleResponse.data.data;
                console.log('✅ 获取示例数据成功');
                console.log('数据统计:');
                console.log(`- 记录数: ${sampleData.count}`);
                console.log(`- 查询耗时: ${sampleData.executionTime}ms`);
                console.log(`- 字段数: ${sampleData.columns ? sampleData.columns.length : 0}`);
                
                if (sampleData.data && sampleData.data.length > 0) {
                    console.log('\n示例记录 (前3条):');
                    sampleData.data.slice(0, 3).forEach((record, index) => {
                        console.log(`第${index + 1}条:`);
                        Object.entries(record).slice(0, 5).forEach(([key, value]) => {
                            console.log(`  ${key}: ${value}`);
                        });
                        console.log('');
                    });
                } else {
                    console.log('📝 表中暂无数据');
                }
            } else {
                console.error('❌ 获取示例数据失败:', sampleResponse.data.message);
            }
        } else {
            console.error('❌ 登录失败:', loginResponse.data.message);
        }
    } catch (error) {
        console.error('❌ 测试异常:', error.message);
    }
}

testApiServicesData();