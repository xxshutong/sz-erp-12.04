/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.wjzpw.service;

import javolution.util.FastMap;
import org.ofbiz.base.util.UtilMisc;
import org.ofbiz.common.FindServices;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.DynamicViewEntity;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.service.DispatchContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MachineOutputService {

    private static Logger logger = LoggerFactory.getLogger(MachineOutputService.class);

    public static Map<String, Object> completeMachineOutput(DispatchContext dctx, Map<String, Object> context) {
        String machineOutputId = (String) context.get("id");
        String machineNo = (String) context.get("machineNo");
        String productId = (String) context.get("productId");
        String batchNoId = (String) context.get("batchNoId");
        logger.info("========================Machine No====>{}", machineNo);
        logger.info("========================Product ID====>{}", productId);
        logger.info("========================Batch No====>{}", batchNoId);
        Delegator delegator = dctx.getDelegator();

        Double total = getTotalOutputAmount(delegator, machineNo, productId, batchNoId);
        // Sum
        try {
            // Update Machine output record
            updateMachineOutput(dctx, machineOutputId, total);
            // Update all input records which have the same batch No and product type
            updateInputByBatchNoProduct(dctx, machineOutputId, machineNo, batchNoId, productId);
        } catch (GenericEntityException e) {
            logger.error(e.getMessage(), e);
        }

        Map<String, Object> finalResult = FastMap.newInstance();

        return finalResult;
    }

    /**
     * 获取累积机台产量
     *
     * @return
     */
    private static Double getTotalOutputAmount(Delegator delegator, String machineNo, String productId, String batchNoId) {
        DynamicViewEntity inputView = new DynamicViewEntity();
        inputView.addMemberEntity("II", "InventoryInput");
        inputView.addAlias("II", "superiorNumber", "superiorNumber", null, null, null, "sum");
        inputView.addAlias("II", "gradeANumber", "gradeANumber", null, null, null, "sum");
        inputView.addAlias("II", "gradeBNumber", "gradeBNumber", null, null, null, "sum");
        inputView.addAlias("II", "machineNo", null, null, null, true, null);
        inputView.addAlias("II", "productId", null, null, null, true, null);
        inputView.addAlias("II", "batchNoId", null, null, null, true, null);
        inputView.addAlias("II", "machineOutputId", null, null, null, true, null);
        // Conditions
        EntityCondition condition = EntityCondition.makeCondition(
                UtilMisc.toList(
                        EntityCondition.makeCondition("machineNo", EntityOperator.EQUALS, machineNo),
                        EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId),
                        EntityCondition.makeCondition("batchNoId", EntityOperator.EQUALS, batchNoId),
                        EntityCondition.makeCondition("machineOutputId", EntityOperator.EQUALS, null)
                ),
                EntityOperator.AND);

        // Sum
        Double total = 0d;
        try {
            EntityListIterator result = delegator.findListIteratorByCondition(inputView, condition, null, null, null, null);
            logger.info(result.toString());
            GenericValue genericValue = result.next();
            if (genericValue != null) {
                if (genericValue.getDouble("superiorNumber") != null) {
                    total += genericValue.getDouble("superiorNumber");
                }
                if (genericValue.getDouble("gradeANumber") != null) {
                    total += genericValue.getDouble("gradeANumber");
                }
                if (genericValue.getDouble("gradeBNumber") != null) {
                    total += genericValue.getDouble("gradeBNumber");
                }
                logger.info("[ERP]==Total output==> {}", total);
            }
            result.close();
        } catch (GenericEntityException e) {
            logger.error(e.getMessage(), e);
        }
        return total;
    }

    /**
     * Complete machine output and calculate the take-up rate
     *
     * @param machineOutputId
     * @param total
     */
    private static void updateMachineOutput(DispatchContext dctx, String machineOutputId, Double total) throws GenericEntityException {
        Delegator delegator = dctx.getDelegator();
        HashMap<String, Object> idMap = new HashMap<String, Object>();
        idMap.put("id", machineOutputId);

        GenericValue machineOutput = delegator.findByPrimaryKey("InventoryMachineOutput", idMap);
        machineOutput.set("outputAmount", total);
        if (machineOutput.getDouble("beamAmount") <= 0d) {
            machineOutput.set("wovenShrinkage", 0d);
        } else {
            machineOutput.set("wovenShrinkage", (machineOutput.getDouble("beamAmount") - total)  / machineOutput.getDouble("beamAmount"));
        }
        machineOutput.set("isCompleted", "Y");
        machineOutput.set("completeDate", new Timestamp(System.currentTimeMillis()));
        machineOutput.store();
    }

    /**
     * Update all related Input Record to completed status
     *
     * @param dctx
     * @param machineOutputId
     * @param machineNo
     * @param batchNoId
     * @param productId
     * @throws GenericEntityException
     */
    private static void updateInputByBatchNoProduct(DispatchContext dctx, String machineOutputId, String machineNo, String batchNoId, String productId) throws GenericEntityException {
        Delegator delegator = dctx.getDelegator();
        HashMap<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("batchNoId", batchNoId);
        paramMap.put("productId", productId);
        paramMap.put("machineNo", machineNo);
        paramMap.put("machineOutputId", null);
        List<GenericValue> resultList = delegator.findByAnd("InventoryInput", paramMap);
        if (resultList != null) {
            for (GenericValue value : resultList) {
                value.set("machineOutputId", machineOutputId);
                value.store();
            }
        }
    }

    /**
     * performFindMachineOutput
     * <p/>
     * This is a generic method that expects entity data affixed with special suffixes
     * to indicate their purpose in formulating an SQL query statement.
     */
    public static Map<String, Object> performFindMachineOutput(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        Map<String, Object> result = FindServices.performFind(dctx, context);
        EntityListIterator iterator = (EntityListIterator) result.get("listIt");
        if (iterator != null) {
            // 计算累计产量
            List<GenericValue> valueList = new ArrayList<GenericValue>();

            GenericValue value = iterator.next();
            while (value != null) {
                if (value.getDouble("outputAmount") == null) {
                    // 计算
                    Double total = getTotalOutputAmount(delegator, (String) value.get("machineNo"), (String) value.get("productId"), (String) value.get("batchNoId"));
                    value.set("outputAmount", total);
                }
                valueList.add(value);
                value = iterator.next();
            }
            result.put("listIt", valueList);
        }
        return result;
    }
}
