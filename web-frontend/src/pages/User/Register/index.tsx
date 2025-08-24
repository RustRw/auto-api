import Footer from '@/components/Footer';
import { register } from '@/services/user';
import { LockOutlined, MailOutlined, UserOutlined } from '@ant-design/icons';
import { LoginForm, ProFormText } from '@ant-design/pro-components';
import { useEmotionCss } from '@ant-design/use-emotion-css';
import { FormattedMessage, Helmet, history, useIntl } from '@umijs/max';
import { Alert, message } from 'antd';
import React, { useState } from 'react';

const RegisterMessage: React.FC<{
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

const Register: React.FC = () => {
  const [registerState, setRegisterState] = useState<API.LoginResult>({});
  const intl = useIntl();

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

  const handleSubmit = async (values: API.RegisterParams) => {
    try {
      // 注册
      const msg = await register(values);
      if (msg.status === 'ok') {
        const defaultRegisterSuccessMessage = intl.formatMessage({
          id: 'pages.register.success',
          defaultMessage: '注册成功！',
        });
        message.success(defaultRegisterSuccessMessage);
        
        // 注册成功后跳转到登录页
        history.push('/user/login');
        return;
      }
      console.log(msg);
      // 如果失败去设置用户错误信息
      setRegisterState(msg);
    } catch (error) {
      const defaultRegisterFailureMessage = intl.formatMessage({
        id: 'pages.register.failure',
        defaultMessage: '注册失败，请重试！',
      });
      console.log(error);
      message.error(defaultRegisterFailureMessage);
    }
  };

  const { status } = registerState;

  return (
    <div className={containerClassName}>
      <Helmet>
        <title>
          {intl.formatMessage({
            id: 'pages.register.title',
            defaultMessage: '注册页',
          })}
          - Auto API Platform
        </title>
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
          subTitle={intl.formatMessage({ 
            id: 'pages.register.subtitle', 
            defaultMessage: '企业级API服务管理平台' 
          })}
          submitter={{
            searchConfig: {
              submitText: intl.formatMessage({
                id: 'pages.register.submit',
                defaultMessage: '注册',
              }),
            },
          }}
          onFinish={async (values) => {
            await handleSubmit(values as API.RegisterParams);
          }}
        >
          {status === 'error' && (
            <RegisterMessage
              content={intl.formatMessage({
                id: 'pages.register.errorMessage',
                defaultMessage: '注册失败，请检查输入信息',
              })}
            />
          )}
          
          <ProFormText
            name="username"
            fieldProps={{
              size: 'large',
              prefix: <UserOutlined />,
            }}
            placeholder={intl.formatMessage({
              id: 'pages.register.username.placeholder',
              defaultMessage: '请输入用户名',
            })}
            rules={[
              {
                required: true,
                message: (
                  <FormattedMessage
                    id="pages.register.username.required"
                    defaultMessage="请输入用户名!"
                  />
                ),
              },
              {
                min: 3,
                message: (
                  <FormattedMessage
                    id="pages.register.username.min"
                    defaultMessage="用户名至少3个字符"
                  />
                ),
              },
            ]}
          />
          
          <ProFormText
            name="email"
            fieldProps={{
              size: 'large',
              prefix: <MailOutlined />,
            }}
            placeholder={intl.formatMessage({
              id: 'pages.register.email.placeholder',
              defaultMessage: '请输入邮箱地址',
            })}
            rules={[
              {
                required: true,
                message: (
                  <FormattedMessage
                    id="pages.register.email.required"
                    defaultMessage="请输入邮箱地址!"
                  />
                ),
              },
              {
                type: 'email',
                message: (
                  <FormattedMessage
                    id="pages.register.email.invalid"
                    defaultMessage="邮箱格式不正确!"
                  />
                ),
              },
            ]}
          />
          
          <ProFormText.Password
            name="password"
            fieldProps={{
              size: 'large',
              prefix: <LockOutlined />,
            }}
            placeholder={intl.formatMessage({
              id: 'pages.register.password.placeholder',
              defaultMessage: '请输入密码',
            })}
            rules={[
              {
                required: true,
                message: (
                  <FormattedMessage
                    id="pages.register.password.required"
                    defaultMessage="请输入密码！"
                  />
                ),
              },
              {
                min: 6,
                message: (
                  <FormattedMessage
                    id="pages.register.password.min"
                    defaultMessage="密码至少6个字符"
                  />
                ),
              },
            ]}
          />
          
          <ProFormText.Password
            name="confirmPassword"
            fieldProps={{
              size: 'large',
              prefix: <LockOutlined />,
            }}
            placeholder={intl.formatMessage({
              id: 'pages.register.confirmPassword.placeholder',
              defaultMessage: '请确认密码',
            })}
            rules={[
              {
                required: true,
                message: (
                  <FormattedMessage
                    id="pages.register.confirmPassword.required"
                    defaultMessage="请确认密码！"
                  />
                ),
              },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(
                    new Error(
                      intl.formatMessage({
                        id: 'pages.register.confirmPassword.mismatch',
                        defaultMessage: '两次输入的密码不一致',
                      })
                    )
                  );
                },
              }),
            ]}
          />

          <div
            style={{
              marginBottom: 24,
              textAlign: 'center',
            }}
          >
            <a
              onClick={() => {
                history.push('/user/login');
              }}
            >
              <FormattedMessage 
                id="pages.register.backToLogin" 
                defaultMessage="已有账户？返回登录" 
              />
            </a>
          </div>
        </LoginForm>
      </div>
      <Footer />
    </div>
  );
};

export default Register;