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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import {
  AbstractControl,
  ControlValueAccessor,
  UntypedFormArray,
  UntypedFormBuilder,
  UntypedFormGroup,
  NG_VALIDATORS,
  NG_VALUE_ACCESSOR,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { Subscription } from 'rxjs';
import { AppState } from '@core/core.state';
import { Store } from '@ngrx/store';
import { MqttQoS, MqttQoSType, mqttQoSTypes } from '@shared/models/session.model';
import { TranslateService } from '@ngx-translate/core';
import { TopicSubscription } from '@shared/models/ws-client.model';

@Component({
  selector: 'tb-session-subscriptions',
  templateUrl: './subscriptions.component.html',
  styleUrls: ['./subscriptions.component.scss'],
  providers: [{
    provide: NG_VALUE_ACCESSOR,
    useExisting: forwardRef(() => SubscriptionsComponent),
    multi: true
  },
    {
      provide: NG_VALIDATORS,
      useExisting: forwardRef(() => SubscriptionsComponent),
      multi: true
    }]
})
export class SubscriptionsComponent extends PageComponent implements ControlValueAccessor, OnInit {

  @Input() disabled: boolean;

  topicListFormGroup: UntypedFormGroup;
  mqttQoSTypes = mqttQoSTypes;

  private propagateChange = (v: any) => {};
  private valueChangeSubscription: Subscription = null;

  constructor(protected store: Store<AppState>,
              private translate: TranslateService,
              private fb: UntypedFormBuilder) {
    super(store);
  }

  ngOnInit(): void {
    this.topicListFormGroup = this.fb.group({});
    this.topicListFormGroup.addControl('subscriptions', this.fb.array([]));
  }

  subscriptionsFormArray(): UntypedFormArray {
    return this.topicListFormGroup.get('subscriptions') as UntypedFormArray;
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState?(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.topicListFormGroup.disable({emitEvent: false});
    }
  }

  writeValue(topics: TopicSubscription[]): void {
    if (this.valueChangeSubscription) {
      this.valueChangeSubscription.unsubscribe();
    }
    const subscriptionsControls: Array<AbstractControl> = [];
    if (topics) {
      for (const topic of topics) {
        const topicControl = this.fb.group(topic);
        subscriptionsControls.push(topicControl);
      }
    }
    this.topicListFormGroup.setControl('subscriptions', this.fb.array(subscriptionsControls));
    this.valueChangeSubscription = this.topicListFormGroup.valueChanges.subscribe(() => {
      this.updateView();
    });
  }

  removeTopic(index: number) {
    (this.subscriptionsFormArray()).removeAt(index);
  }

  addTopic() {
    const group = this.fb.group({
      topicFilter: [null, [Validators.required]],
      qos: [MqttQoS.AT_LEAST_ONCE, []],
      subscriptionId: [null, []],
      options: this.fb.group({
        retainAsPublish: [false, []],
        retainHandling: [0, []],
        noLocal: [false, []],
      })
    });
    this.subscriptionsFormArray().push(group);
  }

  validate(control: AbstractControl): ValidationErrors | null {
    return !this.topicListFormGroup.invalid ? null : {
      topicFilters: {valid: false}
    };
  }

  mqttQoSValue(mqttQoSValue: MqttQoSType): string {
    return this.translate.instant(mqttQoSValue.name);
  }

  subscriptionOptionsChanged(value: TopicSubscription, topicFilter: AbstractControl<TopicSubscription>) {
    topicFilter.patchValue(value);
  }

  private updateView() {
    this.propagateChange(this.topicListFormGroup.getRawValue().subscriptions);
  }
}
