(ns user
  (:require [mount.core :as mount]
            traktor.core))

(defn start []
  (mount/start-without #'traktor.core/repl-server))

(defn stop []
  (mount/stop-except #'traktor.core/repl-server))

(defn restart []
  (stop)
  (start))


