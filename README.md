===
加载数据：
> createdb ofbiz
> ant load-demo

启动
ant start

===
更新ofbiz版本的时候需要搬移的数据
文件:
1. README.md
2. .gitignore
3. sz-erp*/framework/entity/lib/jdbc/postgresql-<version>.jar
4. hot-deploy全目录
配置：
1. 将sz-erp*/framework/entity/config/entityengine.xml中的数据库配置改成postgresql的
2. 修改sz-erp*/framework/common/config/general.properties如下
locales.available=zh
country.geo.id.default=CHINA
3. 修改sz-erp*/framework/start/src/org/ofbiz/base/start/start.properties如下
ofbiz.locale.default=zh
4. 转移framework/common/config/CommonUiLabels.xml中<!-- CUSTOM PART FOR SZ-ERP -->以下部分

