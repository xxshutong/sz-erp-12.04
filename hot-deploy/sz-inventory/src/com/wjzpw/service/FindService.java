package com.wjzpw.service;

import javolution.util.FastList;
import javolution.util.FastMap;
import org.apache.commons.lang.StringUtils;
import org.ofbiz.base.util.UtilMisc;
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

import java.util.List;
import java.util.Map;

import static org.ofbiz.base.util.UtilGenerics.checkMap;

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
     * This is a generic method that expects entity data affixed with special suffixes
     * to indicate their purpose in formulating an SQL query statement.
     */
    public static Map<String, Object> performFindInventory(DispatchContext dctx, Map<String, ?> context) {
        Delegator delegator = dctx.getDelegator();
        Map<String, ?> inputFields = checkMap(context.get("inputFields"), String.class, Object.class); // Input
        String productId = (String) inputFields.get("productId");
        String batchNoId = (String) inputFields.get("batchNoId");
        Integer viewSize = (Integer) context.get("viewSize");
        Integer viewIndex = (Integer) context.get("viewIndex");

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

        // Query
        List<GenericValue> valueList = null;
        Integer listSize = null;
        try {
            int start = viewIndex.intValue() * viewSize.intValue();
            EntityListIterator result = delegator.findListIteratorByCondition(inventoryView, condition, null, null, null, null);
            valueList = result.getPartialList(start, viewSize);
            listSize = result.getResultsSizeAfterPartialList();
            result.close();
        } catch (GenericEntityException e) {
            logger.error(e.getMessage(), e);
        }

        Map<String, Object> results = ServiceUtil.returnSuccess();
        results.put("list", valueList);
        results.put("listSize", listSize);
//        results.put("queryString", prepareResult.get("queryString"));
//        results.put("queryStringMap", prepareResult.get("queryStringMap"));
        return results;
    }

}
