# redis实战项目-类大众点评app
## DAY1
## 1.短信验证码发送功能实现
## 2.用户短信登陆、拦截器部署
### 用户登录请求传递cookie，cookie中携带session id，session中保存了用户信息，通过session中保存的用户信息，可以在前置拦截器中校验登录信息从而显示页面，
### 并将用户信息保存到threadlocal中，从而在controller层中获取到该登录用户并返回完成校验，最后在后置拦截其中销毁threadlocal中的user值从而避免内存泄漏；
