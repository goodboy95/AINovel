import { Link, useNavigate } from 'react-router-dom';
import { Form, Input, Button, Card, Typography, message } from 'antd';
import { UserOutlined, LockOutlined } from '@ant-design/icons';

const { Title } = Typography;

interface LoginValues {
    username: string;
    password?: string;
}

const Login = () => {
    const navigate = useNavigate();

    const onFinish = async (values: LoginValues) => {
        try {
            const response = await fetch('/api/v1/auth/login', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(values),
            });
            const data = await response.json();
            if (response.ok) {
                message.success('登录成功！');
                localStorage.setItem('token', data.token);
                navigate('/story-conception');
            } else {
                console.error('Login failed: Response not OK', data);
                message.error(data.message || '登录失败。');
            }
        } catch (error) {
            console.error('Failed to login:', error);
            message.error('登录时发生错误。');
        }
    };

    return (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', background: '#f0f2f5' }}>
            <Card style={{ width: 400 }}>
                <div style={{ textAlign: 'center', marginBottom: '24px' }}>
                    <Title level={2}>AI 小说家</Title>
                    <Typography.Text type="secondary">登录您的帐户</Typography.Text>
                </div>
                <Form
                    name="normal_login"
                    className="login-form"
                    initialValues={{ remember: true }}
                    onFinish={onFinish}
                >
                    <Form.Item
                        name="username"
                        rules={[{ required: true, message: '请输入您的用户名！' }]}
                    >
                        <Input prefix={<UserOutlined className="site-form-item-icon" />} placeholder="用户名" />
                    </Form.Item>
                    <Form.Item
                        name="password"
                        rules={[{ required: true, message: '请输入您的密码！' }]}
                    >
                        <Input.Password
                            prefix={<LockOutlined className="site-form-item-icon" />}
                            type="password"
                            placeholder="密码"
                        />
                    </Form.Item>

                    <Form.Item>
                        <Button type="primary" htmlType="submit" className="login-form-button" style={{ width: '100%' }}>
                            登录
                        </Button>
                    </Form.Item>
                    或 <Link to="/register">现在注册！</Link>
                </Form>
            </Card>
        </div>
    );
};

export default Login;
