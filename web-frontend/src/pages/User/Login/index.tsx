import Footer from '@/components/Footer';
import { LockOutlined, UserOutlined } from '@ant-design/icons';
import { LoginForm, ProFormCheckbox, ProFormText } from '@ant-design/pro-components';
import { useEmotionCss } from '@ant-design/use-emotion-css';
import { Helmet, history } from '@umijs/max';
import { Alert, message, Tabs } from 'antd';
import React, { useState } from 'react';

const LoginMessage: React.FC<{
  content: string;
}> = ({ content }) => {
  return (
    <Alert
      style={{
        marginBottom: 24,
      }}
      message={content}
      type="error"
      showIcon
    />
  );
};

const Login: React.FC = () => {
  const [userLoginState, setUserLoginState] = useState<any>({});
  const [type, setType] = useState<string>('account');

  const containerClassName = useEmotionCss(() => {
    return {
      display: 'flex',
      flexDirection: 'column',
      height: '100vh',
      overflow: 'auto',
      backgroundImage:
        "url('https://mdn.alipayobjects.com/yuyan_qk0oxh/afts/img/V-_oS6r-i7wAAAAAAAAAAAAAFl94AQBr')",
      backgroundSize: '100% 100%',
    };
  });

  const handleSubmit = async (values: any) => {
    try {
      const { username, password } = values;
      
      // 调用后端登录API
      const response = await fetch('http://localhost:8080/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ username, password })
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const result = await response.json();
      
      if (result.success && result.data?.token) {
        // 保存登录信息到localStorage
        localStorage.setItem('token', result.data.token);
        localStorage.setItem('username', result.data.username);
        localStorage.setItem('role', result.data.role);
        localStorage.setItem('email', result.data.email);
        localStorage.setItem('realName', result.data.realName);
        
        message.success(`登录成功！欢迎 ${result.data.realName || result.data.username}`);
        
        // 跳转到首页
        setTimeout(() => {
          history.push('/welcome');
        }, 1000);
      } else {
        message.error(result.message || '登录失败，请检查用户名和密码');
        setUserLoginState({ status: 'error', type: 'account' });
      }
    } catch (error: any) {
      console.error('Login error:', error);
      if (error.message.includes('Failed to fetch')) {
        message.error('无法连接到服务器，请确保后端服务正在运行');
      } else {
        message.error(`登录失败: ${error.message}`);
      }
      setUserLoginState({ status: 'error', type: 'account' });
    }
  };

  const { status, type: loginType } = userLoginState;

  return (
    <div className={containerClassName}>
      <Helmet>
        <title>登录 - Auto API Platform</title>
      </Helmet>
      <div
        style={{
          flex: '1',
          padding: '32px 0',
        }}
      >
        <LoginForm
          contentStyle={{
            minWidth: 280,
            maxWidth: '75vw',
          }}
          logo={<img alt="logo" src="/logo.svg" />}
          title="Auto API Platform"
          subTitle="企业级API服务管理平台"
          initialValues={{
            autoLogin: true,
          }}
          actions={[
            <div key="register" style={{ textAlign: 'center' }}>
              还没有账号？
              <a
                style={{
                  marginLeft: 8,
                }}
                onClick={() => {
                  history.push('/user/register');
                }}
              >
                立即注册
              </a>
            </div>,
          ]}
          onFinish={async (values) => {
            await handleSubmit(values as any);
          }}
        >
          <Tabs
            activeKey={type}
            onChange={setType}
            centered
            items={[
              {
                key: 'account',
                label: '账户密码登录',
              },
            ]}
          />

          {status === 'error' && loginType === 'account' && (
            <LoginMessage content="账户或密码错误" />
          )}
          {type === 'account' && (
            <>
              <ProFormText
                name="username"
                fieldProps={{
                  size: 'large',
                  prefix: <UserOutlined />,
                }}
                placeholder="用户名: admin 或 user"
                rules={[
                  {
                    required: true,
                    message: '用户名是必填项！',
                  },
                ]}
              />
              <ProFormText.Password
                name="password"
                fieldProps={{
                  size: 'large',
                  prefix: <LockOutlined />,
                }}
                placeholder="密码: admin123 或 user123"
                rules={[
                  {
                    required: true,
                    message: '密码是必填项！',
                  },
                ]}
              />
            </>
          )}

          <div
            style={{
              marginBottom: 24,
            }}
          >
            <ProFormCheckbox noStyle name="autoLogin">
              自动登录
            </ProFormCheckbox>
            <a
              style={{
                float: 'right',
              }}
            >
              忘记密码 ?
            </a>
          </div>
        </LoginForm>
      </div>
      <Footer />
    </div>
  );
};

export default Login;