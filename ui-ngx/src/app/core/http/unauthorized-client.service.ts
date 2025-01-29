///
/// Copyright © 2016-2025 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import { Injectable } from '@angular/core';
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { UnauthorizedClient, UnauthorizedClientQuery } from '@shared/models/unauthorized-client.model';

@Injectable({
  providedIn: 'root'
})
export class UnauthorizedClientService {

  constructor(private http: HttpClient) {
  }

  public getUnauthorizedClients(pageLink: PageLink, config?: RequestConfig): Observable<PageData<UnauthorizedClient>> {
    return this.http.get<PageData<UnauthorizedClient>>(`/api/unauthorized/client${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getUnauthorizedClient(clientId: string, config?: RequestConfig): Observable<UnauthorizedClient> {
    return this.http.get<UnauthorizedClient>(`/api/unauthorized/client?clientId=${encodeURIComponent(clientId)}`, defaultHttpOptionsFromConfig(config));
  }

  public deleteUnauthorizedClient(clientId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/unauthorized/client?clientId=${encodeURIComponent(clientId)}`, defaultHttpOptionsFromConfig(config));
  }

  public deleteAllUnauthorizedClients(config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/unauthorized/client`, defaultHttpOptionsFromConfig(config));
  }

  public getUnauthorizedClientV2(query: UnauthorizedClientQuery, config?: RequestConfig): Observable<PageData<UnauthorizedClient>> {
    return this.http.get<PageData<UnauthorizedClient>>(`/api/unauthorized/client/v2${query.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }
}
