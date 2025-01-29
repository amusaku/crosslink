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

import { Component, ElementRef, forwardRef, Input, NgZone, OnInit, ViewChild } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormBuilder, UntypedFormGroup } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { PageLink } from '@shared/models/page/page-link';
import { Direction } from '@shared/models/page/sort-order';
import { catchError, debounceTime, distinctUntilChanged, map, share, switchMap, tap } from 'rxjs/operators';
import { TranslateService } from '@ngx-translate/core';
import { TruncatePipe } from '@shared//pipe/truncate.pipe';
import { ENTER } from '@angular/cdk/keycodes';
import { MatAutocomplete } from '@angular/material/autocomplete';
import { emptyPageData } from '@shared/models/page/page-data';
import { SubscriptSizing } from '@angular/material/form-field';
import { coerceBoolean } from '@shared/decorators/coercion';
import { ClientCredentials, CredentialsType, wsSystemCredentialsName } from '@shared/models/credentials.model';
import { ClientCredentialsService } from '@core/http/client-credentials.service';
import { WebSocketConnection } from '@shared/models/ws-client.model';
import {
  ClientCredentialsWizardDialogComponent
} from '@home/components/wizard/client-credentials-wizard-dialog.component';
import { AddEntityDialogData } from '@home/models/entity/entity-component.models';
import { MatDialog } from '@angular/material/dialog';
import { EntityTableConfig } from '@home/models/entity/entities-table-config.models';
import { EntityType, entityTypeResources, entityTypeTranslations } from '@shared/models/entity-type.models';
import { ClientCredentialsComponent } from '@home/pages/client-credentials/client-credentials.component';
import { ClientType } from '@shared/models/client.model';
import { ActionNotificationShow } from '@core/notification/notification.actions';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';

@Component({
  selector: 'tb-client-credentials-autocomplete',
  templateUrl: './client-credentials-autocomplete.component.html',
  styleUrls: ['./client-credentials-autocomplete.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => ClientCredentialsAutocompleteComponent),
    multi: true
  }]
})
export class ClientCredentialsAutocompleteComponent implements ControlValueAccessor, OnInit {

  selectCredentialsFormGroup: UntypedFormGroup;

  modelValue: ClientCredentials | null;

  @Input()
  entity: WebSocketConnection;

  @Input()
  subscriptSizing: SubscriptSizing = 'fixed';

  @Input()
  selectDefaultProfile = false;

  @Input()
  selectFirstProfile = false;

  @Input()
  displayAllOnEmpty = false;

  @Input()
  editEnabled = false;

  @Input()
  addNewCredentials = true;

  @Input()
  showDetailsPageLink = false;

  @Input()
  @coerceBoolean()
  required = false;

  @Input()
  disabled: boolean;

  @Input()
  hint: string;

  @ViewChild('clientCredentialsInput', {static: true})
  clientCredentialsInput: ElementRef;

  @ViewChild('clientCredentialsAutocomplete', {static: true})
  clientCredentialsAutocomplete: MatAutocomplete;

  filteredClientCredentials: Observable<Array<ClientCredentials>>;

  searchText = '';

  private dirty = false;

  private ignoreClosedPanel = false;

  private propagateChange = (v: any) => {
  };

  constructor(public translate: TranslateService,
              public truncate: TruncatePipe,
              private dialog: MatDialog,
              private store: Store<AppState>,
              private clientCredentialsService: ClientCredentialsService,
              private fb: UntypedFormBuilder,
              private zone: NgZone) {
    this.selectCredentialsFormGroup = this.fb.group({
      clientCredentials: [null]
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  ngOnInit() {
    this.filteredClientCredentials = this.selectCredentialsFormGroup.get('clientCredentials').valueChanges
      .pipe(
        tap((value: ClientCredentials | string) => {
          let modelValue: ClientCredentials | null;
          if (typeof value === 'string' || !value) {
            modelValue = null;
          } else {
            modelValue = value;
          }
          if (!this.displayAllOnEmpty || modelValue) {
            this.updateView(modelValue?.id);
          }
        }),
        map(value => {
          if (value) {
            if (typeof value === 'string') {
              return value;
            } else {
              if (this.displayAllOnEmpty) {
                return '';
              } else {
                return value.name;
              }
            }
          } else {
            return '';
          }
        }),
        debounceTime(150),
        distinctUntilChanged(),
        switchMap(name => this.fetchClientCredentials(name)),
        share()
      );
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.selectCredentialsFormGroup.disable({emitEvent: false});
    } else {
      this.selectCredentialsFormGroup.enable({emitEvent: false});
    }
  }

  writeValue(credentialsId: string): void {
    this.searchText = '';
    this.dirty = true;
    this.updateView(credentialsId, true);
  }

  onFocus() {
    if (this.dirty) {
      this.selectCredentialsFormGroup.get('clientCredentials').updateValueAndValidity({onlySelf: true, emitEvent: true});
      this.dirty = false;
    }
  }

  onPanelClosed() {
    if (this.ignoreClosedPanel) {
      this.ignoreClosedPanel = false;
    } else {
      if (this.displayAllOnEmpty && !this.selectCredentialsFormGroup.get('clientCredentials').value) {
        this.zone.run(() => {
        }, 0);
      }
    }
  }

  updateView(credentialsId: string, useDefaultCredentials = false) {
    if (credentialsId) {
      this.clientCredentialsService.getClientCredentials(credentialsId).subscribe(
        (credentials) => {
          this.selectCredentialsFormGroup.get('clientCredentials').patchValue(credentials, {emitEvent: false});
          this.propagateChange(credentials);
        }
      );
    } else {
      if (useDefaultCredentials) {
        this.clientCredentialsService.getClientsCredentials(new PageLink(1, 0, wsSystemCredentialsName), {ignoreLoading: true, ignoreErrors: true}).subscribe(
          pageData => {
            const wsSystemCredentials = pageData.data.length ? pageData.data[0] : null;
            if (wsSystemCredentials) {
              this.selectCredentialsFormGroup.get('clientCredentials').patchValue(wsSystemCredentials, {emitEvent: true});
              this.propagateChange(wsSystemCredentials);
            }
          }
        );
      } else {
        this.propagateChange(null);
      }
    }
  }

  displayClientCredentialsFn(credentials?: ClientCredentials): string | undefined {
    return credentials ? credentials.name : undefined;
  }

  fetchClientCredentials(searchText?: string): Observable<Array<ClientCredentials>> {
    this.searchText = searchText;
    const pageLink = new PageLink(100, 0, searchText, {
      property: 'name',
      direction: Direction.ASC
    });
    return this.clientCredentialsService.getClientsCredentials(pageLink, {ignoreLoading: true}).pipe(
      catchError(() => of(emptyPageData<ClientCredentials>())),
      map(pageData => {
        const basicCredentials = pageData.data.filter(el => el.credentialsType === CredentialsType.MQTT_BASIC);
        return basicCredentials;
      })
    );
  }

  clear() {
    this.ignoreClosedPanel = true;
    this.selectCredentialsFormGroup.get('clientCredentials').patchValue(null, {emitEvent: true});
    setTimeout(() => {
      this.clientCredentialsInput.nativeElement.blur();
      this.clientCredentialsInput.nativeElement.focus();
    }, 0);
  }

  textIsNotEmpty(text: string): boolean {
    return (text && text.length > 0);
  }

  clientCredentialsEnter($event: KeyboardEvent) {
    if (this.editEnabled && $event.keyCode === ENTER) {
      $event.preventDefault();
      if (!this.modelValue) {
        this.createClientCredentials($event, this.searchText);
      }
    }
  }

  createClientCredentials($event: Event, credentialsName: string) {
    $event.preventDefault();
    const clientCredentials: ClientCredentials = {
      name: credentialsName
    } as ClientCredentials;
    if (this.addNewCredentials) {
      this.addClientCredentials(clientCredentials);
    }
  }

  addClientCredentials(clientCredentials: ClientCredentials) {
    const config = new EntityTableConfig<ClientCredentials>();
    config.entityType = EntityType.MQTT_CLIENT_CREDENTIALS;
    config.entityComponent = ClientCredentialsComponent;
    config.entityTranslations = entityTypeTranslations.get(EntityType.MQTT_CLIENT_CREDENTIALS);
    config.entityResources = entityTypeResources.get(EntityType.MQTT_CLIENT_CREDENTIALS);
    config.addDialogStyle = {width: 'fit-content'};
    config.demoData = {
      name: clientCredentials.name,
      clientType: ClientType.DEVICE,
      credentialsType: CredentialsType.MQTT_BASIC,
      credentialsValue: JSON.stringify({
        userName: null,
        password: null,
        authRules: {
          pubAuthRulePatterns: ['.*'],
          subAuthRulePatterns: ['.*']
        }
      })
    };
    const $entity = this.dialog.open<ClientCredentialsWizardDialogComponent, AddEntityDialogData<ClientCredentials>,
      ClientCredentials>(ClientCredentialsWizardDialogComponent, {
      disableClose: true,
      panelClass: ['tb-dialog', 'tb-fullscreen-dialog'],
      data: {
        entitiesTableConfig: config
      }
    }).afterClosed();
    $entity.subscribe(
      (entity) => {
        if (entity) {
          this.writeValue(entity.id);
          this.clientCredentialsService.saveClientCredentials(entity).subscribe(() => {
            this.store.dispatch(new ActionNotificationShow(
              {
                message: this.translate.instant('getting-started.credentials-added'),
                type: 'success',
                duration: 1500,
                verticalPosition: 'top',
                horizontalPosition: 'left'
              }));
          });
        }
      }
    );
  }


}
