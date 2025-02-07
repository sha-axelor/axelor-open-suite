/*
 * Axelor Business Solutions
 *
 * Copyright (C) 2023 Axelor (<http://axelor.com>).
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.axelor.apps.account.service.batch;

import com.axelor.apps.account.db.AccountingBatch;
import com.axelor.apps.account.db.Move;
import com.axelor.apps.account.service.move.MoveToolService;
import com.axelor.apps.account.service.move.MoveValidateService;
import com.axelor.db.JPA;
import com.axelor.exception.AxelorException;
import com.axelor.exception.db.TraceBack;
import com.axelor.exception.db.repo.TraceBackRepository;
import com.axelor.exception.service.TraceBackService;
import com.axelor.i18n.I18n;
import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;

public class BatchControlMovesConsistency extends BatchStrategy {

  protected MoveToolService moveToolService;
  protected MoveValidateService moveValidateService;
  protected TraceBackRepository tracebackRepository;

  @Inject
  public BatchControlMovesConsistency(
      MoveToolService moveToolService,
      MoveValidateService moveValidateService,
      TraceBackRepository tracebackRepository) {
    this.moveToolService = moveToolService;
    this.moveValidateService = moveValidateService;
    this.tracebackRepository = tracebackRepository;
  }

  protected void process() {
    AccountingBatch accountingBatch = batch.getAccountingBatch();
    if (!CollectionUtils.isEmpty(accountingBatch.getYearSet())) {
      List<Move> moveList = moveToolService.findDaybookByYear(accountingBatch.getYearSet());
      if (!CollectionUtils.isEmpty(moveList)) {
        for (Move move : moveList) {
          try {
            move = moveRepo.find(move.getId());
            moveValidateService.checkPreconditions(move);
          } catch (AxelorException e) {
            TraceBackService.trace(
                new AxelorException(move, e.getCategory(), I18n.get(e.getMessage())),
                null,
                batch.getId());
            incrementAnomaly();
          } finally {
            JPA.clear();
          }
        }
      }
    }
  }

  public List<Long> getAllMovesId(Long batchId) {
    List<Long> idList = new ArrayList<>();
    List<TraceBack> traceBackList = tracebackRepository.findByBatchId(batchId).fetch();
    if (!CollectionUtils.isEmpty(traceBackList)) {
      for (TraceBack traceBack : traceBackList) {
        if (Move.class.toString().contains(traceBack.getRef()) && traceBack.getRefId() != null) {
          idList.add(traceBack.getRefId());
        }
      }
    }
    return idList;
  }
}
