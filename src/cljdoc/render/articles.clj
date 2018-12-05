(ns cljdoc.render.articles
  "HTML fragments related to rendering articles and article-trees"
  (:require [cljdoc.render.layout :as layout]
            [cljdoc.doc-tree :as doctree]
            [cljdoc.util :as util]
            [cljdoc.util.scm :as scm]
            [cljdoc.server.routes :as routes]
            [clojure.string :as string]
            [hiccup2.core :as hiccup]))

(defn article-list [doc-tree]
  [:div.mb4.js--articles
   (layout/sidebar-title "Articles")
   (or doc-tree
       [:p.pl2.f7.gray
        [:a.blue.link {:href (util/github-url :userguide/articles)} "Articles"]
        " are a practical way to provide additional guidance beyond
       API documentation. To use them, please ensure you "
        [:a.blue.link {:href (util/github-url :userguide/scm-faq)} "properly set SCM info"]
        " in your project."])])

(defn main-list [doc-tree]
  (when doc-tree
    [:div.mb4
     (update doc-tree 0 #(-> %
                             (name)
                             (clojure.string/replace ".pl2" ".pl0")
                             (keyword)))]))

(defn doc-link [cache-id slugs]
  (assert (seq slugs) "Slug path missing")
  (->> (string/join "/" slugs)
       (assoc cache-id :article-slug)
       (routes/url-for :artifact/doc :path-params)))

(defn doc-tree-view
  "Render a set of nested lists representing the doctree. "
  ([cache-id doc-bundle current-page]
   (doc-tree-view cache-id doc-bundle current-page 0))
  ([cache-id doc-bundle current-page level]
   (when (seq doc-bundle)
     (->> doc-bundle
          (map (fn [doc-page]
                 (let [slug-path (-> doc-page :attrs :slug-path)]
                   [:li
                    {:class (when (seq (:children doc-page)) "mv2")}
                    [:a.link.blue.hover-dark-blue.dib.pv1
                     {:style {:word-wrap "break-word"}
                      :href  (doc-link cache-id slug-path)
                      :class (when (= current-page slug-path) "fw7")}
                     (:title doc-page)]
                    (doc-tree-view cache-id (:children doc-page) current-page (inc level))])))
          (into [:ul.list.pl2
                 {:class (when (pos? level) "f6-ns")}])))))

(defn doc-page [{:keys [top-bar-component
                        upgrade-notice-component
                        main-list-component
                        article-list-component
                        namespace-list-component
                        doc-scm-url
                        doc-html] :as args}]
  (layout/layout
   {:top-bar top-bar-component
    :main-sidebar-contents [upgrade-notice-component
                            (main-list main-list-component)
                            (article-list article-list-component)
                            namespace-list-component]
    :content [:div.mw7.center
              ;; TODO dispatch on a type parameter that becomes part of the attrs map
              (if doc-html
                [:div#doc-html.markdown.lh-copy.pv4
                 (hiccup/raw doc-html)
                 [:a.db.f7.tr
                  {:href doc-scm-url}
                  (if (= :gitlab (scm/provider doc-scm-url))
                    "Edit on GitLab"
                    "Edit on GitHub")]]
                [:div.lh-copy.pv6.tc
                 #_[:pre (pr-str (dissoc args :top-bar-component :doc-tree-component :namespace-list-component))]
                 [:span.f4.serif.gray.i "Space intentionally left blank."]])]}))

(defn doc-overview [{:keys [top-bar-component
                            doc-tree-component
                            namespace-list-component
                            cache-id
                            doc-tree] :as args}]
  [:div.doc-page
   (layout/layout
    {:top-bar top-bar-component
     :main-sidebar-contents [(article-list doc-tree-component)
                             namespace-list-component]
     :content [:div.mw7.center.pv4
               [:h1 (:title doc-tree)]
               [:ol
                (for [c (:children doc-tree)]
                  [:li.mv2
                   [:a.link.blue
                    {:href (doc-link cache-id (-> c :attrs :slug-path))}
                    (-> c :title)]])]]})])
