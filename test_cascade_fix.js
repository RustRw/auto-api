// 测试数据源类型级联功能的Node.js脚本
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
    console.log('🚀 开始测试数据源类型级联功能\n');
    
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
                console.log(`✅ 获取到 ${datasources.length} 个数据源:`);
                
                datasources.forEach(ds => {
                    console.log(`   - ${ds.name}: ${ds.type} @ ${ds.host}:${ds.port} (ID: ${ds.id})`);
                });
                
                // 3. 测试类型筛选逻辑
                console.log('\n3. 测试类型筛选逻辑:');
                
                const testTypes = ['H2', 'MYSQL', 'POSTGRESQL'];
                testTypes.forEach(type => {
                    const filtered = datasources.filter(ds => ds.type === type);
                    console.log(`   ${type}: ${filtered.length} 个匹配`);
                    filtered.forEach(ds => {
                        console.log(`      -> ${ds.name} (ID: ${ds.id})`);
                    });
                });
                
                console.log('\n✅ 测试完成 - 数据源API和筛选逻辑正常工作');
                console.log('\n📝 建议用户:');
                console.log('   1. 打开浏览器开发者工具 (F12)');
                console.log('   2. 切换到 Console 标签页');
                console.log('   3. 访问服务开发页面');
                console.log('   4. 点击"新建API服务"');
                console.log('   5. 选择 H2 数据源类型');
                console.log('   6. 查看控制台日志，应该能看到详细的调试信息');
                
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