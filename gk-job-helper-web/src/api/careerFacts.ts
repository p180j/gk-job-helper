import { del, get, post, put } from './http'
import type { CareerFact, CareerFactQuestion } from '@/types/model'
export function fetchCareerFacts(){return get<CareerFact[]>('/api/career-facts')}
export function saveCareerFact(fact:CareerFact){return fact.id?put<CareerFact>(`/api/career-facts/${fact.id}`,fact):post<CareerFact>('/api/career-facts',fact)}
export function deleteCareerFact(id:number){return del<void>(`/api/career-facts/${id}`)}
export function fetchCareerFactQuestions(){return get<CareerFactQuestion[]>('/api/career-facts/questions')}
