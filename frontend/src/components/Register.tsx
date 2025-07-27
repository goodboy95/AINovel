import { Link, useNavigate } from 'react-router-dom';
import { Form, Input, Button, Card, Typography, message } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined } from '@ant-design/icons';

const { Title } = Typography;

interface RegisterValues {
    username: string;
    email: string;
    password?: string;
}

const Register = () => {
    const navigate = useNavigate();

    const onFinish = async (values: RegisterValues) => {
        try {
            const response = await fetch('/api/v1/auth/register', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(values),
            });
            const data = await response.json();
            if (response.ok) {
                message.success('注册成功！现在您可以登录了。');
                navigate('/login');
            } else {
                console.error('Registration failed: Response not OK', data);
                message.error(data.message || '注册失败。');
            }
        } catch (error) {
            console.error('Failed to register:', error);
            message.error('注册时发生错误。');
        }
    };

    return (
        <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', background: '#f0f2f5' }}>
            <Card style={{ width: 400 }}>
                <div style={{ textAlign: 'center', marginBottom: '24px' }}>
                    <Title level={2}>AI 小说家</Title>
                    <Typography.Text type="secondary">创建您的帐户</Typography.Text>
                </div>
                <Form
                    name="register"
                    onFinish={onFinish}
                    scrollToFirstError
                >
                    <Form.Item
                        name="username"
                        rules={[{ required: true, message: '请输入您的用户名！', whitespace: true }]}
                    >
                        <Input prefix={<UserOutlined />} placeholder="用户名" />
                    </Form.Item>
                    <Form.Item
                        name="email"
                        rules={[
                            {
                                type: 'email',
                                message: '输入的电子邮件无效！',
                            },
                            {
                                required: true,
                                message: '请输入您的电子邮件！',
                            },
                        ]}
                    >
                        <Input prefix={<MailOutlined />} placeholder="电子邮件" />
                    </Form.Item>
                    <Form.Item
                        name="password"
                        rules={[
                            {
                                required: true,
                                message: '请输入您的密码！',
                            },
                        ]}
                        hasFeedback
                    >
                        <Input.Password prefix={<LockOutlined />} placeholder="密码" />
                    </Form.Item>

                    <Form.Item
                        name="confirm"
                        dependencies={['password']}
                        hasFeedback
                        rules={[
                            {
                                required: true,
                                message: '请确认您的密码！',
                            },
                            ({ getFieldValue }) => ({
                                validator(_, value) {
                                    if (!value || getFieldValue('password') === value) {
                                        return Promise.resolve();
                                    }
                                    return Promise.reject(new Error('您输入的两个密码不匹配！'));
                                },
                            }),
                        ]}
                    >
                        <Input.Password prefix={<LockOutlined />} placeholder="确认密码" />
                    </Form.Item>

                    <Form.Item>
                        <Button type="primary" htmlType="submit" style={{ width: '100%' }}>
                            注册
                        </Button>
                    </Form.Item>
                    已经有帐户了？ <Link to="/login">登录</Link>
                </Form>
            </Card>
        </div>
    );
};

export default Register;
