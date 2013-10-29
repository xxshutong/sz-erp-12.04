package com.wjzpw.service;

import static org.ofbiz.base.util.UtilGenerics.checkMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import org.apache.commons.lang.StringUtils;
import org.ofbiz.entity.Delegator;
import org.ofbiz.entity.GenericEntityException;
import org.ofbiz.entity.GenericValue;
import org.ofbiz.entity.condition.EntityComparisonOperator;
import org.ofbiz.entity.condition.EntityCondition;
import org.ofbiz.entity.condition.EntityExpr;
import org.ofbiz.entity.condition.EntityOperator;
import org.ofbiz.entity.model.DynamicViewEntity;
import org.ofbiz.entity.util.EntityListIterator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FindService {

    private static Logger logger = LoggerFactory.getLogger(FindService.class);

    public static Map<String, EntityComparisonOperator<?, ?>> entityOperators;

    static {
        entityOperators = FastMap.newInstance();
        entityOperators.put("between", EntityOperator.BETWEEN);
        entityOperators.put("equals", EntityOperator.EQUALS);
        entityOperators.put("greaterThan", EntityOperator.GREATER_THAN);
        entityOperators.put("greaterThanEqualTo", EntityOperator.GREATER_THAN_EQUAL_TO);
        entityOperators.put("in", EntityOperator.IN);
        entityOperators.put("not-in", EntityOperator.NOT_IN);
        entityOperators.put("lessThan", EntityOperator.LESS_THAN);
        entityOperators.put("lessThanEqualTo", EntityOperator.LESS_THAN_EQUAL_TO);
        entityOperators.put("like", EntityOperator.LIKE);
        entityOperators.put("notLike", EntityOperator.NOT_LIKE);
        entityOperators.put("not", EntityOperator.NOT);
        entityOperators.put("notEqual", EntityOperator.NOT_EQUAL);
    }

    /**
     * performFind
     * 
     * This is a generic method that expects entity data affixed with special suffixes to indicate their purpose in
     * formulating an SQL query statement.
     */
    public static Map<String, Object> performFindInventory(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        Map<String, ?> inputFields = checkMap(context.get("inputFields"), String.class, Object.class); // Input
        String productId = (String) inputFields.get("productId");
        String batchNoId = (String) inputFields.get("batchNoId");
        Integer viewSize = (Integer) context.get("viewSize");
        Integer viewIndex = (Integer) context.get("viewIndex");
        String orderBy = (String) context.get("orderBy");

        DynamicViewEntity inventoryView = new DynamicViewEntity();
        inventoryView.addMemberEntity("II", "InventoryInput");
        inventoryView.addAlias("II", "superiorNumber", "superiorNumber", null, null, null, "sum");
        inventoryView.addAlias("II", "gradeANumber", "gradeANumber", null, null, null, "sum");
        inventoryView.addAlias("II", "gradeBNumber", "gradeBNumber", null, null, null, "sum");
        inventoryView.addAlias("II", "productId", null, null, null, true, null);
        inventoryView.addAlias("II", "batchNoId", null, null, null, true, null);

        // Conditions
        List<EntityExpr> expList = FastList.newInstance();
        if (!StringUtils.isBlank(productId)) {
            expList.add(EntityCondition.makeCondition("productId", EntityOperator.EQUALS, productId));
        }
        if (!StringUtils.isBlank(batchNoId)) {
            expList.add(EntityCondition.makeCondition("batchNoId", EntityOperator.EQUALS, batchNoId));
        }

        EntityCondition condition = EntityCondition.makeCondition(expList, EntityOperator.AND);

        // Order by
        List<String> orderBys = new ArrayList<String>();
        orderBys.add(orderBy);

        // Query
        List<GenericValue> valueList = null;
        Integer listSize = null;
        try {
            int start = viewIndex.intValue() * viewSize.intValue();
            EntityListIterator result = delegator.findListIteratorByCondition(inventoryView, condition, null, null,
                    orderBys, null);
            valueList = result.getPartialList(start, viewSize);
            listSize = result.getResultsSizeAfterPartialList();
            result.close();
            // Calculate inventory
            valueList = calculateInventory(delegator, valueList, condition);
        } catch (GenericEntityException e) {
            logger.error(e.getMessage(), e);
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("list", valueList);
        results.put("listSize", listSize);
        // results.put("queryString", prepareResult.get("queryString"));
        // results.put("queryStringMap", prepareResult.get("queryStringMap"));
        return results;
    }

    /**
     * Calculate inventory by total input minus total output
     */
    private static List<GenericValue> calculateInventory(Delegator delegator, List<GenericValue> valueList, EntityCondition condition)
            throws GenericEntityException {
        // TODO optimize this function in future
        // Query
        DynamicViewEntity outputView = new DynamicViewEntity();
        outputView.addMemberEntity("IO", "InventoryOutput");
        outputView.addAlias("IO", "superiorNumber", "superiorNumber", null, null, null, "sum");
        outputView.addAlias("IO", "gradeANumber", "gradeANumber", null, null, null, "sum");
        outputView.addAlias("IO", "gradeBNumber", "gradeBNumber", null, null, null, "sum");
        outputView.addAlias("IO", "productId", null, null, null, true, null);
        outputView.addAlias("IO", "batchNoId", null, null, null, true, null);
        EntityListIterator result = delegator
                .findListIteratorByCondition(outputView, condition, null, null, null, null);
        List<GenericValue> outputList = result.getCompleteList();

        // Calculate
        for (GenericValue value : valueList) {
            for (GenericValue outputValue : outputList) {
                if (value.get("productId") != null && value.get("productId").equals(outputValue.get("productId"))
                        && value.get("batchNoId") != null
                        && value.get("batchNoId").equals(outputValue.get("batchNoId"))) {
                    value.set("superiorNumber", inputMinusOutput(value.getDouble("superiorNumber"), outputValue.getDouble("superiorNumber")));
                    value.set("gradeANumber", inputMinusOutput(value.getDouble("gradeANumber"), outputValue.getDouble("gradeANumber")));
                    value.set("gradeBNumber", inputMinusOutput(value.getDouble("gradeBNumber"), outputValue.getDouble("gradeBNumber")));
                }
            }
        }
        return valueList;
    }

    private static Double inputMinusOutput(Double input, Double output) {
        if (input == null && output == null) {
            return null;
        }
        if (input == null && output != null) {
            return 0 - output;
        }
        if (input != null && output == null) {
            return input;
        }
        return  input - output;
    }

}
