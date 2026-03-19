rootProject.name = "modular-erp"

// Platform
include("platform:core")
include("platform:security")
include("platform:i18n")
include("platform:messaging")
include("platform:web")
include("platform:preference")
include("platform:admin")
include("platform:audit")
include("platform:report")
include("platform:ai")

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
include("modules:production")
include("modules:planning")
include("modules:batch")
include("modules:notification")
include("modules:budget")
include("modules:asset")
include("modules:period-close")
include("modules:crm")
include("modules:costing")
include("modules:currency")

// Application
include("app")
