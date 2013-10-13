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
5. 修改sz-erp*/framework/common/widget/CommonScreens.xml
从<set field="initialLocaleComplete" type="String" value="${groovy:parameters?.userLogin?.lastLocale}" default-value="${groovy:locale.toString()}"/>
到<set field="initialLocaleComplete" type="String" value="zh_CN" default-value="${groovy:locale.toString()}"/>
注：此为临时解决办法，以后需完美修复
6. 修改sz-erp*/framework/common/widget/CommonScreens.xml
从localeFiles.put("zh_CN", "/images/jquery/plugins/validate/localization/messages_en.js");
到localeFiles.put("zh_CN", "/images/jquery/plugins/validate/localization/messages_cn.js");


