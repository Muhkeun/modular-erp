rootProject.name = "modular-erp"

// Platform
include("platform:core")
include("platform:security")
include("platform:i18n")
include("platform:messaging")
include("platform:web")

// Business Modules
include("modules:master-data")
include("modules:approval")
include("modules:document")
include("modules:sales")
include("modules:purchase")
include("modules:logistics")
include("modules:account")
include("modules:hr")
include("modules:quality")
include("modules:supply-chain")
include("modules:contract")

// Application
include("app")
