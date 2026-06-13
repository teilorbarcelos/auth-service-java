package com.app.modules.dashboard;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import java.util.List;

@RegisterForReflection
public class DashboardSchemas {

    @Schema(name = "TimeSeriesStat")
    @RegisterForReflection
    public static class TimeSeriesStatDto {
        @Schema(examples = {"2026-05-23"})
        public String date;

        @Schema(examples = {"10"})
        public Integer count;

        public TimeSeriesStatDto() {}

        public TimeSeriesStatDto(String date, Integer count) {
            this.date = date;
            this.count = count;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }
    }

    @Schema(name = "UserProductStat")
    @RegisterForReflection
    public static class UserProductStatDto {
        @Schema(examples = {"user-uuid-123"})
        public String userId;

        @Schema(examples = {"João Silva"})
        public String userName;

        @Schema(examples = {"5"})
        public Integer count;

        public UserProductStatDto() {}

        public UserProductStatDto(String userId, String userName, Integer count) {
            this.userId = userId;
            this.userName = userName;
            this.count = count;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }
    }

    @Schema(name = "DashboardStatsResponse")
    @RegisterForReflection
    public static class DashboardStatsResponseDto {
        public List<TimeSeriesStatDto> userCreationStats;
        public List<TimeSeriesStatDto> productCreationStats;
        public List<UserProductStatDto> productsPerUser;

        public List<TimeSeriesStatDto> getUserCreationStats() {
            return userCreationStats;
        }

        public void setUserCreationStats(List<TimeSeriesStatDto> userCreationStats) {
            this.userCreationStats = userCreationStats;
        }

        public List<TimeSeriesStatDto> getProductCreationStats() {
            return productCreationStats;
        }

        public void setProductCreationStats(List<TimeSeriesStatDto> productCreationStats) {
            this.productCreationStats = productCreationStats;
        }

        public List<UserProductStatDto> getProductsPerUser() {
            return productsPerUser;
        }

        public void setProductsPerUser(List<UserProductStatDto> productsPerUser) {
            this.productsPerUser = productsPerUser;
        }
    }
}
