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

import { Component, Input, OnInit, ViewChild } from '@angular/core';
import { EntitiesTableComponent } from '@home/components/entity/entities-table.component';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { RetainedMessagesTableConfig } from '@home/pages/retained-messages/retained-messages-table-config';
import { DialogService } from '@core/services/dialog.service';
import { RetainedMsgService } from '@core/http/retained-msg.service';
import { MatDialog } from '@angular/material/dialog';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'tb-retained-messages-table',
  templateUrl: './retained-messages-table.component.html'
})
export class RetainedMessagesTableComponent implements OnInit {

  @Input()
  detailsMode: boolean;

  activeValue = false;
  dirtyValue = false;
  entityIdValue: string;

  @Input()
  set active(active: boolean) {
    if (this.activeValue !== active) {
      this.activeValue = active;
      if (this.activeValue && this.dirtyValue) {
        this.dirtyValue = false;
        this.entitiesTable.updateData();
      }
    }
  }

  @Input()
  set entityId(entityId: string) {
    this.entityIdValue = entityId;
    if (this.retainedMessagesTableConfig && this.retainedMessagesTableConfig.entityId !== entityId) {
      this.retainedMessagesTableConfig.entityId = entityId;
      this.entitiesTable.resetSortAndFilter(this.activeValue);
      if (!this.activeValue) {
        this.dirtyValue = true;
      }
    }
  }

  @ViewChild(EntitiesTableComponent, {static: true}) entitiesTable: EntitiesTableComponent;

  retainedMessagesTableConfig: RetainedMessagesTableConfig;

  constructor(private dialogService: DialogService,
              private retainedMsgService: RetainedMsgService,
              private translate: TranslateService,
              private dialog: MatDialog,
              private datePipe: DatePipe,
              private route: ActivatedRoute) {
  }

  ngOnInit(): void {
    this.dirtyValue = !this.activeValue;
    this.retainedMessagesTableConfig = new RetainedMessagesTableConfig(
      this.dialogService,
      this.retainedMsgService,
      this.translate,
      this.dialog,
      this.datePipe,
      this.entityIdValue,
      this.route
    );
  }

}
