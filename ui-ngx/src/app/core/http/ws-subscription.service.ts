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
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { WebSocketSubscription } from '@shared/models/ws-client.model';

@Injectable({
  providedIn: 'root'
})
export class WebSocketSubscriptionService {

  constructor(private http: HttpClient) {
  }

  public saveWebSocketSubscription(subscription: WebSocketSubscription, config?: RequestConfig): Observable<WebSocketSubscription> {
    return this.http.post<WebSocketSubscription>('/api/ws/subscription', subscription, defaultHttpOptionsFromConfig(config));
  }

  public getWebSocketSubscriptions(webSocketConnectionId: string, config?: RequestConfig): Observable<WebSocketSubscription[]> {
    return this.http.get<WebSocketSubscription[]>(`/api/ws/subscription?webSocketConnectionId=${webSocketConnectionId}`, defaultHttpOptionsFromConfig(config));
  }

  public getWebSocketSubscriptionById(webSocketSubscriptionId: string, config?: RequestConfig): Observable<WebSocketSubscription> {
    return this.http.get<WebSocketSubscription>(`/api/ws/subscription/${webSocketSubscriptionId}`, defaultHttpOptionsFromConfig(config));
  }

  public deleteWebSocketSubscription(webSocketSubscriptionId: string, config?: RequestConfig) {
    return this.http.delete(`/api/ws/subscription/${webSocketSubscriptionId}`, defaultHttpOptionsFromConfig(config));
  }
}
