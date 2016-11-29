(ns traktor.env
  (:require [selmer.parser :as parser]
            [clojure.tools.logging :as log]
            [traktor.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[traktor started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[traktor has shut down successfully]=-"))
   :middleware wrap-dev})
