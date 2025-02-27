provider "azurerm" {
  version = "1.23.0"
}
locals {
  app_full_name = "${var.product}-${var.component}"
  ase_name = "core-compute-${var.env}"
  local_env = "${(var.env == "preview" || var.env == "spreview") ? (var.env == "preview" ) ? "aat" : "saat" : var.env}"
  shared_vault_name = "${var.shared_product_name}-${local.local_env}"
}
# "${local.ase_name}"
# "${local.app_full_name}"
# "${local.local_env}"

module "app" {
  source = "git@github.com:hmcts/cnp-module-webapp?ref=master"
  product = "${local.app_full_name}"
  location = "${var.location}"
  env = "${var.env}"
  ilbIp = "${var.ilbIp}"
  subscription = "${var.subscription}"
  capacity     = "${var.capacity}"
  is_frontend = false
  additional_host_name = "${local.app_full_name}-${var.env}.service.${var.env}.platform.hmcts.net"
  https_only="false"
  common_tags  = "${var.common_tags}"
  asp_rg = "${var.shared_product_name}-${var.env}"
  asp_name = "${var.shared_product_name}-bundling-${var.env}"
  enable_ase                      = "${var.enable_ase}"

  app_settings = {
    # JAVA_OPTS = "${var.java_opts}"
    # SERVER_PORT = "8080"

    # idam
    IDAM_API_BASE_URI = "${var.idam_api_base_uri}"
    S2S_BASE_URI = "http://${var.s2s_url}-${local.local_env}.service.core-compute-${local.local_env}.internal"
    S2S_KEY = "${data.azurerm_key_vault_secret.s2s_key.value}"

    #DM STORE
    DM_STORE_APP_URL = "http://${var.dm_store_app_url}-${local.local_env}.service.core-compute-${local.local_env}.internal"
    #EM ANN API
    EM_STITCHING_API_URL = "http://${var.em_stitching_api_url}-${local.local_env}.service.core-compute-${local.local_env}.internal"

    # logging vars & healthcheck
    REFORM_SERVICE_NAME = "${local.app_full_name}"
    REFORM_TEAM = "${var.team_name}"
    REFORM_SERVICE_TYPE = "${var.app_language}"
    REFORM_ENVIRONMENT = "${var.env}"

    PACKAGES_NAME = "${local.app_full_name}"
    PACKAGES_PROJECT = "${var.team_name}"
    PACKAGES_ENVIRONMENT = "${var.env}"

    JSON_CONSOLE_PRETTY_PRINT = "${var.json_console_pretty_print}"
    LOG_OUTPUT = "${var.log_output}"

    # addtional log
    ROOT_LOGGING_LEVEL = "${var.root_logging_level}"
    LOG_LEVEL_SPRING_WEB = "${var.log_level_spring_web}"
    LOG_LEVEL_DM = "${var.log_level_dm}"
    SHOW_SQL = "${var.show_sql}"

    ENDPOINTS_HEALTH_SENSITIVE = "${var.endpoints_health_sensitive}"
    ENDPOINTS_INFO_SENSITIVE = "${var.endpoints_info_sensitive}"

    # Toggles
    ENABLE_IDAM_HEALTH_CHECK = "${var.enable_idam_healthcheck}"
    ENABLE_S2S_HEALTH_CHECK = "${var.enable_s2s_healthcheck}"

    CALLBACK_HTTP_SCHEME = "http"
    CALLBACK_HTTP_HOST_PORT = "80"
    CALLBACK_DOMAIN = "em-ccd-orchestrator-${local.local_env}.service.core-compute-${local.local_env}.internal"

    CCD_DATA_API_URL = "http://${var.ccd_data_store_api_url}-${local.local_env}.service.core-compute-${local.local_env}.internal"

    ENABLE_STITCHING_COMPLETE_CALLBACK="${var.enable_stitching_complete_callback}"
    managed_identity_object_id = "${var.managed_identity_object_id}"

  }
}

provider "vault" {
  address = "https://vault.reform.hmcts.net:6200"
}

data "azurerm_key_vault" "s2s_vault" {
  name = "s2s-${local.local_env}"
  resource_group_name = "rpe-service-auth-provider-${local.local_env}"
}

data "azurerm_key_vault_secret" "s2s_key" {
  name      = "microservicekey-em-ccd-orchestrator"
  key_vault_id = "${data.azurerm_key_vault.s2s_vault.id}"
}

data "azurerm_key_vault" "shared_key_vault" {
  name = "${local.shared_vault_name}"
  resource_group_name = "${local.shared_vault_name}"
}

module "local_key_vault" {
  source = "git@github.com:hmcts/cnp-module-key-vault?ref=master"
  product = "${local.app_full_name}"
  env = "${var.env}"
  tenant_id = "${var.tenant_id}"
  object_id = "${var.jenkins_AAD_objectId}"
  resource_group_name = "${module.app.resource_group_name}"
  product_group_object_id = "5d9cd025-a293-4b97-a0e5-6f43efce02c0"
  common_tags = "${var.common_tags}"
  managed_identity_object_id = "${var.managed_identity_object_id}"
}

# Copy s2s key from shared to local vault
data "azurerm_key_vault" "local_key_vault" {
  name = "${module.local_key_vault.key_vault_name}"
  resource_group_name = "${module.local_key_vault.key_vault_name}"
}

resource "azurerm_key_vault_secret" "local_s2s_key" {
  name         = "microservicekey-em-ccd-orchestrator"
  value        = "${data.azurerm_key_vault_secret.s2s_key.value}"
  key_vault_id = "${data.azurerm_key_vault.local_key_vault.id}"
}
