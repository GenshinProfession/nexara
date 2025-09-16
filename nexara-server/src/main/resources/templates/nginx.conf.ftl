server {
    listen 80;
    server_name localhost;
    root /usr/share/nginx/html;

    <#-- 遍历前端应用 -->
<#list frontends as frontend>
location ${frontend.accessPath}/ {
        alias /usr/share/nginx/html/frontend-${frontend.index}/;
        index index.html index.htm;
        try_files $uri $uri/ /index.html;
    }
</#list>
}