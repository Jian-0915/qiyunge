package com.qiyunge.ui.admin;

import com.qiyunge.app.AppContext;
import com.qiyunge.domain.entity.AuditLog;
import com.qiyunge.domain.entity.RegistrationRequest;
import com.qiyunge.domain.entity.User;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

public class AdminView extends VBox {

    private final AppContext appContext;
    private final AdminViewModel viewModel;

    public AdminView(AppContext appContext) {
        this.appContext = appContext;
        this.viewModel = new AdminViewModel(appContext);
        this.setPadding(new Insets(24));
        this.setSpacing(20);
        this.getStyleClass().add("admin-view");

        // Page header
        Label title = new Label("阁务司");
        title.getStyleClass().add("admin-section-title");
        Label subtitle = new Label("管理用户名册、审批入阁申请、查阅行迹簿");
        subtitle.getStyleClass().add("admin-subtitle");
        VBox header = new VBox(4, title, subtitle);

        // Stats cards
        HBox statsRow = createStatsRow();

        // TabPane: 名册 / 待审批 / 行迹簿
        TabPane tabPane = new TabPane();
        tabPane.getStyleClass().add("admin-tab-pane");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab usersTab = new Tab("名册", createUsersTab());
        Tab requestsTab = new Tab("待审批", createRequestsTab());
        Tab logsTab = new Tab("行迹簿", createLogsTab());

        tabPane.getTabs().addAll(usersTab, requestsTab, logsTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        // Error message
        Label errorLabel = new Label();
        errorLabel.textProperty().bind(viewModel.errorMessageProperty());
        errorLabel.getStyleClass().add("admin-error");
        errorLabel.visibleProperty().bind(viewModel.errorMessageProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        this.getChildren().addAll(header, statsRow, tabPane, errorLabel);

        // Refresh stats when data changes
        viewModel.setOnDataChanged(this::updateStats);
    }

    // ===== 统计卡片 =====

    private HBox createStatsRow() {
        HBox row = new HBox(16);
        row.getStyleClass().add("admin-stats-row");

        row.getChildren().addAll(
            createStatCard("总用户数", viewModel.totalUsersProperty()),
            createStatCard("正常用户", viewModel.activeUsersProperty()),
            createStatCard("禁用用户", viewModel.disabledUsersProperty()),
            createStatCard("待审批", viewModel.pendingRequestsProperty())
        );
        return row;
    }

    private VBox createStatCard(String label, javafx.beans.property.IntegerProperty valueProp) {
        VBox card = new VBox(4);
        card.getStyleClass().add("stat-card");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(16));
        card.setPrefWidth(140);

        Label valueLabel = new Label();
        valueLabel.textProperty().bind(Bindings.convert(valueProp));
        valueLabel.getStyleClass().add("stat-value");

        Label nameLabel = new Label(label);
        nameLabel.getStyleClass().add("stat-label");

        card.getChildren().addAll(valueLabel, nameLabel);
        return card;
    }

    private void updateStats() {
        // Statistics are bound via properties, no manual update needed
    }

    // ===== 名册 Tab =====

    private VBox createUsersTab() {
        VBox tab = new VBox(12);
        tab.getStyleClass().add("admin-tab-content");

        // Toolbar
        HBox toolbar = new HBox(12);
        toolbar.getStyleClass().add("admin-toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);

        TextField searchField = new TextField();
        searchField.setPromptText("搜索用户名或昵称...");
        searchField.getStyleClass().add("search-box");
        searchField.setPrefWidth(220);
        searchField.textProperty().bindBidirectional(viewModel.keywordProperty());

        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("全部", "正常", "禁用");
        statusCombo.getSelectionModel().select(0);
        statusCombo.getStyleClass().add("app-combo-box");
        statusCombo.valueProperty().addListener((obs, old, val) -> {
            String filter = switch (val) {
                case "正常" -> "active";
                case "禁用" -> "disabled";
                default -> "all";
            };
            viewModel.statusFilterProperty().set(filter);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        toolbar.getChildren().addAll(searchField, statusCombo, spacer);

        // User table
        TableView<User> table = new TableView<>();
        table.getStyleClass().add("app-table-view");
        table.setItems(viewModel.getFilteredUsers());
        table.setPlaceholder(new Label("暂无用户"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<User, String> usernameCol = new TableColumn<>("用户名");
        usernameCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getUsername()));
        usernameCol.setPrefWidth(120);
        usernameCol.setMinWidth(80);

        TableColumn<User, String> displayCol = new TableColumn<>("昵称");
        displayCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().getDisplayName() != null ? d.getValue().getDisplayName() : "-"));
        displayCol.setPrefWidth(100);
        displayCol.setMinWidth(60);

        TableColumn<User, String> roleCol = new TableColumn<>("角色");
        roleCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().isAdmin() ? "管理员" : "普通用户"));
        roleCol.setPrefWidth(80);
        roleCol.setMinWidth(60);

        TableColumn<User, String> statusCol = new TableColumn<>("状态");
        statusCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().isDisabled() ? "禁用" : "正常"));
        statusCol.setPrefWidth(70);
        statusCol.setMinWidth(50);
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().addAll("status-badge", item.equals("禁用") ? "disabled" : "active");
                    setGraphic(badge);
                }
            }
        });

        TableColumn<User, String> createdCol = new TableColumn<>("创建时间");
        createdCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().toString() : "-"));
        createdCol.setPrefWidth(170);
        createdCol.setMinWidth(140);

        TableColumn<User, String> loginCol = new TableColumn<>("最后登录");
        loginCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().getLastLoginAt() != null ? d.getValue().getLastLoginAt().toString() : "-"));
        loginCol.setPrefWidth(170);
        loginCol.setMinWidth(140);

        TableColumn<User, Void> actionCol = new TableColumn<>("操作");
        actionCol.setPrefWidth(200);
        actionCol.setMinWidth(180);
        actionCol.setMaxWidth(260);
        actionCol.setCellFactory(col -> new TableCell<>() {
            private final HBox actions = new HBox(6);
            private final Button disableBtn = new Button("禁用");
            private final Button enableBtn = new Button("启用");
            private final Button resetBtn = new Button("重置密码");

            {
                actions.setAlignment(Pos.CENTER_LEFT);
                disableBtn.getStyleClass().addAll("app-button", "button-danger");
                enableBtn.getStyleClass().addAll("app-button", "button-primary");
                resetBtn.getStyleClass().addAll("app-button", "button-secondary");
                disableBtn.setPrefWidth(60);
                enableBtn.setPrefWidth(60);
                resetBtn.setPrefWidth(80);

                disableBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    if (appContext.getDialogService().showConfirm("禁用用户",
                        "确定要禁用用户 \"" + user.getUsername() + "\" 吗？")) {
                        viewModel.disableUser(user.getId());
                    }
                });
                enableBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    if (appContext.getDialogService().showConfirm("启用用户",
                        "确定要启用用户 \"" + user.getUsername() + "\" 吗？")) {
                        viewModel.enableUser(user.getId());
                    }
                });
                resetBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    if (appContext.getDialogService().showConfirm("重置密码",
                        "确定要将用户 \"" + user.getUsername() + "\" 的密码重置为 123456 吗？用户下次登录需要修改密码。")) {
                        viewModel.resetPassword(user.getId());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                User user = getTableView().getItems().get(getIndex());
                actions.getChildren().clear();
                if (user.isDisabled()) {
                    actions.getChildren().add(enableBtn);
                } else {
                    actions.getChildren().add(disableBtn);
                }
                actions.getChildren().add(resetBtn);
                setGraphic(actions);
            }
        });

        @SuppressWarnings("unchecked")
        TableColumn<User, ?>[] cols = new TableColumn[] { usernameCol, displayCol, roleCol, statusCol, createdCol, loginCol, actionCol };
        table.getColumns().addAll(cols);
        VBox.setVgrow(table, Priority.ALWAYS);

        tab.getChildren().addAll(toolbar, table);
        return tab;
    }

    // ===== 待审批 Tab =====

    private VBox createRequestsTab() {
        VBox tab = new VBox(12);
        tab.getStyleClass().add("admin-tab-content");

        Label title = new Label("入阁申请");
        title.getStyleClass().add("admin-tab-title");

        TableView<RegistrationRequest> table = new TableView<>();
        table.getStyleClass().add("app-table-view");
        table.setItems(viewModel.getRequests());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("暂无待审批的入阁申请"));

        TableColumn<RegistrationRequest, String> reqUserCol = new TableColumn<>("用户名");
        reqUserCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(d.getValue().getUsername()));
        reqUserCol.setPrefWidth(130);
        reqUserCol.setMinWidth(90);

        TableColumn<RegistrationRequest, String> reqNameCol = new TableColumn<>("昵称");
        reqNameCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().getDisplayName() != null ? d.getValue().getDisplayName() : "-"));
        reqNameCol.setPrefWidth(120);
        reqNameCol.setMinWidth(80);

        TableColumn<RegistrationRequest, String> reqReasonCol = new TableColumn<>("申请理由");
        reqReasonCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().getReason() != null ? d.getValue().getReason() : "-"));
        reqReasonCol.setPrefWidth(260);
        reqReasonCol.setMinWidth(160);

        TableColumn<RegistrationRequest, String> reqTimeCol = new TableColumn<>("申请时间");
        reqTimeCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().toString() : "-"));
        reqTimeCol.setPrefWidth(180);
        reqTimeCol.setMinWidth(150);

        TableColumn<RegistrationRequest, Void> reqActionCol = new TableColumn<>("操作");
        reqActionCol.setPrefWidth(180);
        reqActionCol.setMinWidth(150);
        reqActionCol.setMaxWidth(220);
        reqActionCol.setCellFactory(col -> new TableCell<>() {
            private final HBox actions = new HBox(8);
            private final Button approveBtn = new Button("通过");
            private final Button rejectBtn = new Button("拒绝");

            {
                actions.setAlignment(Pos.CENTER_LEFT);
                approveBtn.getStyleClass().addAll("app-button", "button-primary");
                rejectBtn.getStyleClass().addAll("app-button", "button-danger");
                approveBtn.setPrefWidth(64);
                rejectBtn.setPrefWidth(64);

                approveBtn.setOnAction(e -> {
                    RegistrationRequest req = getTableView().getItems().get(getIndex());
                    if (appContext.getDialogService().showConfirm("通过申请",
                        "确定要通过 \"" + req.getUsername() + "\" 的入阁申请吗？")) {
                        viewModel.approveRequest(req.getId());
                    }
                });
                rejectBtn.setOnAction(e -> {
                    RegistrationRequest req = getTableView().getItems().get(getIndex());
                    if (appContext.getDialogService().showConfirm("拒绝申请",
                        "确定要拒绝 \"" + req.getUsername() + "\" 的入阁申请吗？")) {
                        viewModel.rejectRequest(req.getId());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    return;
                }
                actions.getChildren().setAll(approveBtn, rejectBtn);
                setGraphic(actions);
            }
        });

        @SuppressWarnings("unchecked")
        TableColumn<RegistrationRequest, ?>[] cols = new TableColumn[] { reqUserCol, reqNameCol, reqReasonCol, reqTimeCol, reqActionCol };
        table.getColumns().addAll(cols);
        VBox.setVgrow(table, Priority.ALWAYS);

        tab.getChildren().addAll(title, table);
        return tab;
    }

    // ===== 行迹簿 Tab =====

    private VBox createLogsTab() {
        VBox tab = new VBox(12);
        tab.getStyleClass().add("admin-tab-content");

        Label title = new Label("近期操作记录");
        title.getStyleClass().add("admin-tab-title");

        TableView<AuditLog> table = new TableView<>();
        table.getStyleClass().add("app-table-view");
        table.setItems(viewModel.getLogs());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(new Label("暂无操作记录"));

        TableColumn<AuditLog, String> logActionCol = new TableColumn<>("动作");
        logActionCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().getActionDisplay()));

        TableColumn<AuditLog, String> logDetailCol = new TableColumn<>("详情");
        logDetailCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().getDetail() != null ? d.getValue().getDetail() : "-"));

        TableColumn<AuditLog, String> logTimeCol = new TableColumn<>("时间");
        logTimeCol.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(
            d.getValue().getCreatedAt() != null ? d.getValue().getCreatedAt().toString() : "-"));

        @SuppressWarnings("unchecked")
        TableColumn<AuditLog, ?>[] cols = new TableColumn[] { logActionCol, logDetailCol, logTimeCol };
        table.getColumns().addAll(cols);
        VBox.setVgrow(table, Priority.ALWAYS);

        tab.getChildren().addAll(title, table);
        return tab;
    }
}
