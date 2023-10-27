package com.wiseasy.ecr.hub.sdk.model.response;

import com.alibaba.fastjson2.annotation.JSONField;

import java.util.List;

public class PaymentInitResponse extends ECRHubResponse {
    /**
     * Payment method category list
     */
    @JSONField(name = "payMethodCategoryList")
    private List<PayMethodCategory> pay_method_category_list;


    public List<PayMethodCategory> getPay_method_category_list() {
        return pay_method_category_list;
    }

    public void setPay_method_category_list(List<PayMethodCategory> pay_method_category_list) {
        this.pay_method_category_list = pay_method_category_list;
    }

    public static class PayMethodCategory {
        /**
         * Payment method category
         */
        @JSONField(name = "payMethodCategory")
        private String pay_method_category;
        /**
         * Payment method list
         */
        @JSONField(name = "payMethodList")
        private List<PayMethod> pay_method_list;

        public String getPay_method_category() {
            return pay_method_category;
        }

        public void setPay_method_category(String pay_method_category) {
            this.pay_method_category = pay_method_category;
        }

        public List<PayMethod> getPay_method_list() {
            return pay_method_list;
        }

        public void setPay_method_list(List<PayMethod> pay_method_list) {
            this.pay_method_list = pay_method_list;
        }

        public static class PayMethod {
            /**
             * Payment method id
             *
             * Example: Visa
             */
            @JSONField(name = "payMethodId")
            private String pay_method_id;
            /**
             * Payment method name
             *
             * Example: Visa
             */
            @JSONField(name = "payMethodName")
            private String pay_method_name;
            /**
             * Support transaction type list
             *
             * Example: ["1", "2", "3", ...]
             */
            @JSONField(name = "supportedTransTypeList")
            private List<String> supported_trans_type_list;

            public String getPay_method_id() {
                return pay_method_id;
            }

            public void setPay_method_id(String pay_method_id) {
                this.pay_method_id = pay_method_id;
            }

            public String getPay_method_name() {
                return pay_method_name;
            }

            public void setPay_method_name(String pay_method_name) {
                this.pay_method_name = pay_method_name;
            }

            public List<String> getSupported_trans_type_list() {
                return supported_trans_type_list;
            }

            public void setSupported_trans_type_list(List<String> supported_trans_type_list) {
                this.supported_trans_type_list = supported_trans_type_list;
            }
        }
    }
}