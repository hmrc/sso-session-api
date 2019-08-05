# sso-frontend


[![Build Status](https://ci-dev.tax.service.gov.uk/buildStatus/icon?job=sso-frontend)](https://ci-dev.tax.service.gov.uk/view/GG/job/sso-frontend/)

Micro service providing SSO functionality between the portal and the MDTP platform.


| Path                                                | Methods | Description                                                                       |
|:----------------------------------------------------|:--------|:----------------------------------------------------------------------------------|
|/sso-api/web-session?continueUrl=:continueUrl        | GET     | Returns an absolute url to the web-session                                        |
|/sso/session?token=:a-token                          | GET     | Redirects to the continueUrl previously stored with the BT and session restored   |
|/sso/:guid/transparent.png  			                    | GET     | Extend the MDTP session                                                           |
|/sso/ssoin/:id?continueUrl=:continueUrl              | GET     | Redirects to continue Url with BT if sessionInfo Id is found                      |

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

**GET** `/sso/session?token=:a-token`

Redirects to the continueUrl previously stored with the BT and session restored

Responds with:

| Status   | Message                                       |
| :------: |-----------------------------------------------|
| 303      | Redirect to the continueUrl                   |
| 404      | Invalid token, token not found                |

**GET** `/sso/session?token=a-token`

_No response body_

Redirect to the continueUrl

**GET** `/sso/:guid/transparent.png`

Returns 200 with transparent 1x1 png image and extend the current MDTP session if it exists

  Responds with:

| Status   | Message                                       |
| :------: |-----------------------------------------------|
| 200      | Return transparent 1x1 png image              |

**GET** `/sso/extend-session/123ABC/transparent.png`

The guid can be anything and should change for every request to avoid image caching

_No response body_

**GET** `/sso/ssoin/:id?continueUrl=:continueUrl`

Redirects to continue Url with BT if sessionInfo Id is found

Responds with:

| Status   | Message                                       |
| :------: |-----------------------------------------------|
| 303      | Redirect to the continueUrl                   |
| 404      | Invalid id, sessionInfo not found             |

**GET** `/sso/ssoin/5927f77c81639f4400484281?continueUrl=/account`


Example response
_No response body_

Redirect to the continueUrl
	
