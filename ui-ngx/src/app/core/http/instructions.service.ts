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
import { Observable, of } from 'rxjs';
import { HttpClient } from '@angular/common/http';

@Injectable({
  providedIn: 'root'
})
export class InstructionsService {

  constructor(
    private http: HttpClient
  ) {
  }

  public getInstruction(id: string): Observable<string> {
    return this.http.get(`/assets/getting-started/${id}.md`, { responseType: 'text' });
  }

  public setInstructionsList(basicAuthEnabled: boolean): Observable<Array<any>> {
    const steps = [
      {
        id: 'client-app',
        title: 'getting-started.step-client-app'
      },
      {
        id: 'client-device',
        title: 'getting-started.step-client-dev'
      },
      {
        id: 'subscribe',
        title: 'getting-started.step-subscribe'
      },
      {
        id: 'publish',
        title: 'getting-started.step-publish'
      },
      {
        id: 'session',
        title: 'getting-started.step-session'
      }
    ];
    if (!basicAuthEnabled) {
      steps.unshift(
        {
            id: 'enable-basic-auth',
            title: 'getting-started.step-enable-basic-auth'
          }
        );
    }
    // @ts-ignore
    steps.map((el, index) => el.position = index + 1);
    return of(steps);
  }

}
