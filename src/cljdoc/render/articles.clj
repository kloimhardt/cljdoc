(ns cljdoc.render.articles
  "HTML fragments related to rendering articles and article-trees"
  (:require [cljdoc.util.scm :as scm]
            [cljdoc.server.routes :as routes]
            [clojure.string :as string]
            [hiccup2.core :as hiccup]))

(defn doc-link [version-entity slugs]
  (assert (seq slugs) "Slug path missing")
  (->> (string/join "/" slugs)
       (assoc version-entity :article-slug)
       (routes/url-for :artifact/doc :path-params)))

(defn doc-tree-view
  "Render a set of nested lists representing the doctree. "
  ([version-entity doc-bundle current-page]
   (doc-tree-view version-entity doc-bundle current-page 0))
  ([version-entity doc-bundle current-page level]
   (when (seq doc-bundle)
     (->> doc-bundle
          (map (fn [doc-page]
                 (let [slug-path (-> doc-page :attrs :slug-path)]
                   [:li
                    {:class (when (seq (:children doc-page)) "mv2")}
                    [:a.link.blue.hover-dark-blue.dib.pv1
                     {:style {:word-wrap "break-word"}
                      :href  (doc-link version-entity slug-path)
                      :class (when (= current-page slug-path) "fw7")}
                     (:title doc-page)]
                    (doc-tree-view version-entity (:children doc-page) current-page (inc level))])))
          (into [:ul.list.ma0 {:class (if (pos? level) "f6-ns pl2" "pl0")}])))))

(defn doc-page
  [{:keys [doc-scm-url doc-html doc-type contributors]}]
  (assert doc-type)
  [:div.mw7.center
   (if doc-html
     [:div#doc-html.cljdoc-article.lh-copy.pv4
      {:class (name doc-type)}
      (hiccup/raw doc-html)]
     [:div.lh-copy.pv6.tc
      [:span.f4.serif.gray.i "Space intentionally left blank."]])
   ;; outside of div with markdown specific styling so markdown
   ;; styling does not override tachyons classes.
   (when doc-html
     [:div.bt.b--light-gray.pv4.f7.cf
      [:p.lh-copy.w-60-ns.ma0.tr.fr
       (if (< 1 (count contributors))
         [:span.db
          [:b "Can you improve this documentation?"] " These fine people already did:" [:br]
          (string/join ", " (butlast contributors))
          " & " (last contributors)]
         [:b.db "Can you improve this documentation?"])
       [:a.link.dib.white.bg-blue.ph2.pv1.br2.mt2
        {:href doc-scm-url}
        (if (= :gitlab (scm/provider doc-scm-url))
          "Edit on GitLab"
          "Edit on GitHub")]]])])

(defn doc-overview
  [{:keys [version-entity doc-tree]}]
  [:div.doc-page
   [:div.mw7.center.pv4
    [:h1 (:title doc-tree)]
    [:ol
     (for [c (:children doc-tree)]
       [:li.mv2
        [:a.link.blue
         {:href (doc-link version-entity (-> c :attrs :slug-path))}
         (-> c :title)]])]]])
