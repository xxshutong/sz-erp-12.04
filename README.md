===
加载数据：
> createdb ofbiz
> ant load-demo

启动
ant start

===
更新ofbiz版本的时候需要搬移的数据
文件:
README.md
.gitignore
sz-erp*/framework/entity/lib/jdbc/postgresql-<version>.jar
hot-deploy全目录
配置：
将sz-erp*/framework/entity/config/entityengine.xml中的数据库配置改成postgresql的