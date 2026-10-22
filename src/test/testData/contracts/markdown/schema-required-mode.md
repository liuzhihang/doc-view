**接口名称:**

postSchema

**接口描述:**



**请求路径:**

/api/schema

**请求方式:**

POST

**请求参数:**

- Header

|参数名|参数值|必填|描述|版本|
|:-----|:-----|:-----|:-----|:-----|
|Content-Type|application/json|Y|||


- Param



- Body

|参数名|类型|必选|描述|版本|
|:-----|:-----|:-----|:-----|:-----|
|displayName|String|Y|显示名称||
|memo|String|N|可选内容||


**请求示例:**

```JSON
{
    "displayName": "",
    "memo": ""
}
```

**返回参数:**

|参数名|类型|必选|描述|版本|
|:-----|:-----|:-----|:-----|:-----|
|displayName|String|Y|显示名称||
|memo|String|N|可选内容||


**返回示例:**

```JSON
{
    "displayName": "",
    "memo": ""
}
```
