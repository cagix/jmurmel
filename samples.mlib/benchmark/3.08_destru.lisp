;;; Performance and Evaluation of Lisp Systems, Richard P. Gabriel, 1985
;;; p 82

#+murmel (require "mlib")
(load "bench.lisp")


;;;  DESTRU  --  Destructive  operation  benchmark
(defun destructive (n m)
  (let ((l (do ((i 10 (1- i))
                (a () (push () a)))
               ((= i 0) a))))
    (do ((i n (1- i)))
        ((= i 0))
      (cond ((null (car l))
             (do ((l l (cdr l)))
                 ((null l))
               (or (car l)
                   (rplaca l (cons () ())))
               (nconc (car l)
                      (do ((j m (1- j))
                           (a () (push () a)))
                          ((= j 0) a)))))
            (t
             (do ((l1 l (cdr l1))
                  (l2 (cdr l) (cdr l2)))
                 ((null l2))
               (rplacd (do ((j (floor (length (car l2)) 2)
                                      (1- j))
                            (a (car l2) (cdr a)))
                           ((zerop j) a)
                         (rplaca a i))
                       (let ((n (floor (length (car l1)) 2)))
                         (cond ((= n 0) (rplaca l1 ())
                                (car l1))
                               (t
                                (do ((j n (1- j))
                                     (a (car l1) (cdr a)))
                                    ((= j 1)
                                     (prog1 (cdr a)
                                            (rplacd a ())))
                                  (rplaca a i))))))))))))
;;; call:  (destructive 600. 50.)

(bench "destru" (destructive 600 50) *default-duration*)