// 测试数据查看功能的API端点
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

async function testDataViewFunctionality() {
    console.log('🧪 测试数据查看功能\n');
    
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
            
            // 2. 获取数据库列表
            console.log('\n2. 获取数据源1的数据库列表...');
            const databasesResponse = await makeRequest(`${API_BASE_URL}/api/datasources/1/databases`, {
                method: 'GET',
                headers: { 
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json' 
                }
            });
            
            console.log('数据库列表响应状态:', databasesResponse.status);
            if (databasesResponse.data.success) {
                console.log('✅ 获取数据库列表成功');
                console.log('数据库列表:', databasesResponse.data.data);
                
                const databases = databasesResponse.data.data;
                if (databases && databases.length > 0) {
                    const firstDatabase = databases[0];
                    
                    // 3. 获取表列表
                    console.log(`\n3. 获取数据库"${firstDatabase}"的表列表...`);
                    const tablesResponse = await makeRequest(`${API_BASE_URL}/api/datasources/1/databases/${encodeURIComponent(firstDatabase)}/tables`, {
                        method: 'GET',
                        headers: { 
                            'Authorization': `Bearer ${token}`,
                            'Content-Type': 'application/json' 
                        }
                    });
                    
                    console.log('表列表响应状态:', tablesResponse.status);
                    if (tablesResponse.data.success) {
                        console.log('✅ 获取表列表成功');
                        console.log('表列表:', tablesResponse.data.data);
                        
                        const tables = tablesResponse.data.data;
                        if (tables && tables.length > 0) {
                            const firstTable = tables[0];
                            
                            // 4. 获取表结构
                            console.log(`\n4. 获取表"${firstTable}"的结构...`);
                            const structureResponse = await makeRequest(`${API_BASE_URL}/api/datasources/1/databases/${encodeURIComponent(firstDatabase)}/tables/${encodeURIComponent(firstTable)}/structure`, {
                                method: 'GET',
                                headers: { 
                                    'Authorization': `Bearer ${token}`,
                                    'Content-Type': 'application/json' 
                                }
                            });
                            
                            console.log('表结构响应状态:', structureResponse.status);
                            if (structureResponse.data.success) {
                                console.log('✅ 获取表结构成功');
                                console.log('表结构:', JSON.stringify(structureResponse.data.data, null, 2));
                            } else {
                                console.error('❌ 获取表结构失败:', structureResponse.data.message);
                            }
                            
                            // 5. 获取示例数据
                            console.log(`\n5. 获取表"${firstTable}"的示例数据...`);
                            const sampleResponse = await makeRequest(`${API_BASE_URL}/api/datasources/1/databases/${encodeURIComponent(firstDatabase)}/tables/${encodeURIComponent(firstTable)}/sample-data?limit=5`, {
                                method: 'GET',
                                headers: { 
                                    'Authorization': `Bearer ${token}`,
                                    'Content-Type': 'application/json' 
                                }
                            });
                            
                            console.log('示例数据响应状态:', sampleResponse.status);
                            if (sampleResponse.data.success) {
                                console.log('✅ 获取示例数据成功');
                                const sampleData = sampleResponse.data.data;
                                console.log('数据统计:');
                                console.log(`- 记录数: ${sampleData.count}`);
                                console.log(`- 查询耗时: ${sampleData.executionTime}ms`);
                                console.log(`- 字段列表: ${sampleData.columns ? sampleData.columns.join(', ') : '无'}`);
                                console.log('示例数据:', JSON.stringify(sampleData.data, null, 2));
                            } else {
                                console.error('❌ 获取示例数据失败:', sampleResponse.data.message);
                            }
                        } else {
                            console.log('ℹ️  数据库中暂无表');
                        }
                    } else {
                        console.error('❌ 获取表列表失败:', tablesResponse.data.message);
                    }
                } else {
                    console.log('ℹ️  数据源中暂无数据库');
                }
            } else {
                console.error('❌ 获取数据库列表失败:', databasesResponse.data.message);
            }
            
            // 6. 测试搜索功能
            console.log('\n6. 测试数据库搜索功能...');
            const searchResponse = await makeRequest(`${API_BASE_URL}/api/datasources/1/databases?search=auto`, {
                method: 'GET',
                headers: { 
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json' 
                }
            });
            
            console.log('搜索响应状态:', searchResponse.status);
            if (searchResponse.data.success) {
                console.log('✅ 数据库搜索功能正常');
                console.log('搜索结果:', searchResponse.data.data);
            } else {
                console.error('❌ 数据库搜索失败:', searchResponse.data.message);
            }
            
        } else {
            console.error('❌ 登录失败:', loginResponse.data.message);
        }
    } catch (error) {
        console.error('❌ 测试异常:', error.message);
    }
}

testDataViewFunctionality();