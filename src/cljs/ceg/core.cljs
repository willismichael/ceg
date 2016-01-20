(ns ceg.core
  (:require [cljsjs.three]))

(def THREE js/THREE)

(defonce canvas (.getElementById js/document "canvas"))
(defonce scene (new THREE.Scene))
(defonce cam (new THREE.PerspectiveCamera 75
                  (/ (aget js/window "innerWidth")
                     (aget js/window "innerHeight"))
                  .1
                  1000))

(defonce renderer (new THREE.WebGLRenderer (js-obj {"canvas" canvas})))
(defonce tex (new THREE.ImageUtils.loadTexture "img/stone.png"))
(defonce tex-mat (new THREE.MeshBasicMaterial (js-obj "map" tex)))

(defonce sector-mesh (atom nil))

(defn create-sector-geo 
  "
  FIXME:  Won't work for sectors with more than one block!
  Vertex indices can't be fixed numbers, they depend on
  which block they belong to
  "
  [sector]
  (let [geo (new THREE.Geometry)]
    (doseq [[[x y z] block] (:blocks sector)]
      (doseq [[vx vy vz] [[     x       y       z]
                          [(inc x)      y       z]
                          [     x  (inc y)      z]
                          [(inc x) (inc y)      z]
                          [     x       y  (inc z)]
                          [(inc x)      y  (inc z)]
                          [     x  (inc y) (inc z)]
                          [(inc x) (inc y) (inc z)]]]
        (-> geo (aget "vertices") (.push (new THREE.Vector4 vx vy vz))))

      (doseq [[v1 v2 v3] [;; north    ;;  2  3
                          [0 2 1]     ;;
                          [1 2 3]     ;;  0  1
                          
                          ;; south    ;;  6  7
                          [4 5 6]     ;;
                          [5 7 6]     ;;  4  5

                          ;; east 
                          [5 1 7]
                          [7 1 3]

                          ;; west 
                          [0 4 2]
                          [2 4 6]

                          ;; top 
                          [6 7 2]
                          [2 7 3]

                          ;; bottom
                          [0 1 4]
                          [4 1 5]
                          ]


              ] 
        (-> geo (aget "faces") (.push (new THREE.Face3 v1 v2 v3)))))
    
    (.computeFaceNormals geo)

    geo))

(defn update-scene [scene sector]
  (.remove scene "sector-blocks")
  (when @sector-mesh
    (.remove scene @sector-mesh))

  (let [geo (create-sector-geo sector)
        mesh (new THREE.Mesh geo tex-mat)]
    (aset mesh "name" "sector-blocks")
    (reset! sector-mesh mesh)
    (.add scene mesh)))





(defn do-animate []
  (when @sector-mesh
    (let [cam-rot (aget cam "rotation")
          cam-y-rot (aget cam-rot "y")
          sm @sector-mesh
          sm-rot (aget sm "rotation")
          sm-rot-x (aget sm-rot "x")
          sm-rot-y (aget sm-rot "y")]
      (aset sm-rot "x" (+ sm-rot-x 0.02))
      (aset sm-rot "y" (+ sm-rot-y 0.01))
      ;; (aset cam-rot "y" (+ cam-y-rot 0.01))
      ))
    (.render renderer scene cam)
)

(defn animate []
  (js/requestAnimationFrame animate)
  (do-animate))

(defn resize []
  (let [screen-width  (aget js/window "innerWidth")
        screen-height (aget js/window "innerHeight")
        render-width  (/ screen-width  2)
        render-height (/ screen-height 2)]
    (.setSize renderer render-width render-height false)
    (aset cam "aspect" (/ render-width render-height))
    (.updateProjectionMatrix cam)))

(defn init!
  []
  (aset (aget cam "position") "z" 10)

  (update-scene scene {:blocks {[2 0 0] :stone}})

  (resize)

  (.appendChild (aget js/document "body") (aget renderer "domElement"))
  (animate))

(aset js/window "onresize" resize)
