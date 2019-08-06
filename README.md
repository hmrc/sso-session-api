# sso-session-api

Micro service providing SSO functionality between the portal and the MDTP platform.

| Path                                                | Methods | Description                                                                       |
|:----------------------------------------------------|:--------|:----------------------------------------------------------------------------------|
|/sso-api/web-session?continueUrl=:continueUrl        | GET     | Returns an absolute url to the web-session                                        |


**GET** `/sso-api/web-session?continueUrl=:continueUrl`

Requires Bearer token and Session-Id in the headers and returns a hal+json response with url _links section
that if opened in a browser will create a web session with the same access as the current api user has.


Responds with:

| Status   | Message                                                                   |
| :------: |---------------------------------------------------------------------------|
| 200      | HAL body containing the url to redeem the token                           |
| 401      | Unauthorized, no bearer token in the authorisation header, invalid BT     |

**GET** `/sso-api/web-session?continueUrl=/tax-credits/change-circumstances`

_NOTE: requires Bearer token and session id_ 

```
{
  "_links" : {
    "session": "http://www.tax.service.goiv.uk/sso/session?token=flurdyflurdyflurdyflurdyflurdyflurdyflurdy
  }
}
```
